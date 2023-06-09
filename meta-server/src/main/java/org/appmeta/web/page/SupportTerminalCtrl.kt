package org.appmeta.web.page

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.appmeta.H
import org.appmeta.Role
import org.appmeta.component.deploy.Deployer
import org.appmeta.domain.AppVersion
import org.appmeta.domain.AppVersionMapper
import org.appmeta.domain.TerminalLogMapper
import org.appmeta.model.IdStringModel
import org.appmeta.model.QueryModel
import org.appmeta.service.TerminalService
import org.nerve.boot.FileStore
import org.springframework.util.Assert
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files

@RestController
@RequestMapping("page/terminal")
class SupportTerminalCtrl (
    private val versionM:AppVersionMapper,
    private val fileStore: FileStore,
    private val deployer: Deployer,
    private val service: TerminalService, private val logM:TerminalLogMapper):BasicPageCtrl() {

    @RequestMapping("log-{aid}", name = "按应用查询后端服务记录")
    fun logList(@RequestBody model: QueryModel, @PathVariable aid:String) = result {
        Assert.hasText(aid, "应用ID不能为空")

        it.data = service.logList(model.form, model.pagination, aid)
        it.total= model.pagination.total
    }

    @PostMapping("log-overview", name = "单应用后端服务总览")
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

        val terminal = service.load(page.aid)?: throw Exception("应用⌈${page.aid}⌋未开通后端服务或者未初始化")

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
}