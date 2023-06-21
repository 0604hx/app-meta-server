package org.appmeta.web.page

import com.alibaba.fastjson2.JSON
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.appmeta.F
import org.appmeta.H
import org.appmeta.Role
import org.appmeta.component.AppConfig
import org.appmeta.component.deploy.Deployer
import org.appmeta.domain.AppVersion
import org.appmeta.domain.AppVersionMapper
import org.appmeta.domain.TerminalLogMapper
import org.appmeta.model.FieldModel
import org.appmeta.model.IdStringModel
import org.appmeta.model.QueryModel
import org.appmeta.service.TerminalService
import org.appmeta.tool.FileTool
import org.nerve.boot.FileStore
import org.nerve.boot.Result
import org.nerve.boot.module.operation.Operation
import org.springframework.util.Assert
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

@RestController
@RequestMapping("page/terminal")
class SupportTerminalCtrl (
    private val config: AppConfig,
    private val versionM:AppVersionMapper,
    private val fileStore: FileStore,
    private val deployer: Deployer,
    private val service: TerminalService, private val logM:TerminalLogMapper):BasicPageCtrl() {

    private fun _load(aid: String) = service.load(aid)?: throw Exception("应用⌈${aid}⌋未开通后端服务或者未初始化")

    @PostMapping("overview", name = "后端服务运行状态")
    fun overview(@RequestBody model: IdStringModel) = _checkEditResult(_load(model.id).pid) { _, _->
        deployer.overview().find { it.name == model.id }
    }

    @PostMapping("restart", name = "重启后端服务")
    fun restart(@RequestBody model: IdStringModel) = _checkEditResult(_load(model.id).pid) { _, _->
        deployer.restart(model.id)
    }

    @PostMapping("stop", name = "停止后端服务")
    fun stop(@RequestBody model: IdStringModel) = _checkEditResult(_load(model.id).pid) { _, _->
        deployer.stop(model.id)
    }

    @RequestMapping("trace-{aid}", name = "按应用查询后端服务记录")
    fun logList(@RequestBody model: QueryModel, @PathVariable aid:String) = _checkEditAuth(_load(aid).pid) {_, _ ->
        Result().also {
            it.data = service.logList(model.form, model.pagination, aid)
            it.total= model.pagination.total
        }
    }

    @PostMapping("trace-overview", name = "单应用后端服务总览")
    fun logOverview(@RequestBody model:IdStringModel) = resultWithData { service.logOverview(model.id) }

    val VALID_EXTS = listOf("js", "jar", "zip")

    @PostMapping("deploy", name = "部署应用服务")
    fun deploy(@RequestPart("file") file: MultipartFile, version: AppVersion) = result {
        val user = authHolder.get()
        Assert.isTrue(H.hasAnyRole(user, Role.DEPLOYER, Role.ADMIN), "仅限 ${Role.DEPLOYER} 角色或者管理员进行部署操作")

        val page = pageM.selectById(version.pid)
        if(!authHelper.checkEdit(page, user))
            throw Exception("您不具备编辑该页面/功能的权限")


        val ext = FilenameUtils.getExtension(file.originalFilename)
        Assert.isTrue(VALID_EXTS.contains(ext), "仅支持 $VALID_EXTS 格式（当前为 $ext）")

        val terminal = _load(page.aid)

        deployer.checkRequirement()

        //"TERMINAL-${DateUtil.getDateTimeSimple()}.${ext}"
        //保留原始文件名
        val path = fileStore.buildPathWithoutDate("${page.aid}/${file.originalFilename}", FileStore.TEMP)
        if(!Files.exists(path.parent))
            Files.createDirectories(path.parent)
        val codeFile = path.toFile()

        FileUtils.copyToFile(file.inputStream, codeFile)

        deployer.deploy(page.aid, codeFile, terminal)

        version.uid  = user.showName
        version.path = codeFile.path
        version.size = codeFile.length()
        if(!StringUtils.hasText(version.version))
            version.version = H.buildVersion()
        version.addOn = System.currentTimeMillis()

        versionM.insert(version)
    }

    /**
     * 构建目录或者文件的信息
     */
    private fun _buildFileItem(f:Path) = f.isDirectory().let { isDir->
        mapOf(
            F.NAME  to f.name,
            F.TYPE  to if(isDir) 0 else 1,
            "size"  to if(isDir) 0 else f.fileSize(),
            F.TIME  to f.getLastModifiedTime().toString()
        )
    }

    /**
     * 参数：
     *  id      应用id
     *  key     路径，默认为 /
     *          如果目标是文件夹，则显示其结构：
     *              name    名称
     *              type    0 = 目录，1=文件
     *              size    文件大小（byte）
     *              time    最后修改时间
     *          如果是文件，则读取前 10 行内容
     *
     *  value   操作类型，download 则为下载，否则为显示内容
     */
    @PostMapping("file", name = "显示部署目录结构或下载文件")
    fun directoryOrDownload(@RequestBody model:FieldModel, response: HttpServletResponse):Unit =
        _checkServiceAuth(_load(model.id as String).pid) { page, user ->
            val path = model.key.let { dir->
                val p = if(StringUtils.hasText(dir)) dir else ""
                var root = Paths.get(config.terminalPath, page.aid)
                logger.info("${user.showName} 尝试访问 应用#${page.aid} 的文件 $p")
                var target = root.resolve(p)

                if(!target.startsWith(root))    throw Exception("非法路径 $p")

                target
            }
            if(!path.exists())  throw Exception("应用#${page.aid}不存在文件/目录：$path")

            val isFile = path.isRegularFile()

            if(model.value == "download"){
                if(!isFile) throw Exception("应用#${page.aid}下 $path 是一个目录，不支持下载")

                downloadFile(response, path.toFile(), path.name) {
                    opLog("${user.showName} 在 $requestIP 下载应用#${page.aid} 的文件 $path", null, Operation.EXPORT)
                }
            }
            else{
                initResponse(response)
                write(response, JSON.toJSONString(
                    Result().setData(
                        mapOf(
                            "file"      to _buildFileItem(path),
                            F.CONTENT   to if(isFile) FileTool.readLines(path, false) else {
                                Files.list(path).map { _buildFileItem(it) }.toList()
                            }
                        )
                    )
                ))
            }
        }
}