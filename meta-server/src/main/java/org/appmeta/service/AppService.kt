package org.appmeta.service

import org.apache.commons.io.FileUtils
import org.appmeta.*
import org.appmeta.component.AppConfig
import org.appmeta.domain.*
import org.appmeta.model.AppModel
import org.appmeta.tool.FileTool
import org.nerve.boot.Const.EMPTY
import org.nerve.boot.db.service.BaseService
import org.nerve.boot.exception.ServiceException
import org.nerve.boot.module.setting.SettingService
import org.nerve.boot.util.DateUtil
import org.nerve.boot.util.MD5Util
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.FileSystemUtils
import org.springframework.util.StreamUtils
import org.springframework.util.StringUtils
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.zip.ZipFile


/*
 * @project app-meta-server
 * @file    org.appmeta.service.AppService
 * CREATE   2022年12月06日 13:33 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@Service
class AppRoleService:BaseService<AppRoleMapper, AppRole>(){

    @Cacheable(Caches.APP_ACCOUNT)
    fun isAdmin(aid:String, uid:String) = count(Q().eq(F.AID, aid).eq(F.UID, uid).eq(F.ROLE, Role.ADMIN)) > 0L
}

@Service
class AppLinkService(
    private val appAsync:AppAsync,
    private val appM:AppMapper):BaseService<AppLinkMapper, AppLink>() {

    private fun buildQ(link: AppLink) = Q().eq(F.AID, link.aid).eq(F.UID, link.uid).eq(F.TYPE, link.type)

    /**
     * 根据用户id跟类型查询对应的应用列表
     */
    fun byUid(uid: String, type:Int, formIndex:Int=0, size:Int = 20):List<App> {
        val q = Q().eq(F.UID, uid).eq(F.TYPE, type)
        q.last("LIMIT ${formIndex},$size")

        return list(q).mapNotNull { appM.withCache(it.aid) }
    }

    fun exist(link: AppLink) = count(buildQ(link)) > 0L

    fun create(link: AppLink){
        if(org.apache.commons.lang3.StringUtils.isAnyEmpty(link.aid, link.uid))
            throw ServiceException("关联的应用、用户不能为空")

        if(count(buildQ(link)) > 0L)    return
//            throw ServiceException("重复关联")

        save(link)
        logger.info("创建 ${link.uid} 与应用#${link.aid} 的关联（类型=${link.type}）")

        if(link.type == AppLink.MARK)       appAsync.afterMark(link.aid)
        else if(link.type == AppLink.LIKE)  appAsync.afterLike(link.aid)
    }

    fun remove(link: AppLink) {
        if(link.id>0L){
            if(StringUtils.hasText(link.uid))
                remove(Q().eq(F.UID, link.uid).eq(F.ID, link.id))
            else
                removeById(link.id)

            logger.info("按ID#${link.id} 删除关联")
        }
        else{
            remove(buildQ(link))
            logger.info("删除 ${link.uid} 与应用#${link.aid} 的关联（类型=${link.type}）")

            if(link.type == AppLink.MARK)   appAsync.afterMark(link.aid, true)
        }
    }
}


@Service
class AppVersionService(private val config:AppConfig, private val settingService: SettingService):BaseService<AppVersionMapper, AppVersion>() {
    private val FAIL = "文件检验不通过"
    private val SIGN = "SIGN"

    /**
     * 返回 SIGN 文件中的清单列表
     */
    fun checkZipFile(file:File): List<String> {
        ZipFile(file).use { zipFile->
            //必须包含首页文件
            zipFile.getEntry(config.home)?: throw Exception("资源包下必须包含 ${config.home} 文件")

            val signEntry = zipFile.getEntry(SIGN) ?: throw Exception(FAIL)

            val zis = zipFile.getInputStream(signEntry)
            val sign = StreamUtils.copyToString(zis, StandardCharsets.UTF_8)
            if(sign.trim().isEmpty())   throw Exception(FAIL)

            val c = Calendar.getInstance()
            var fileMd5 = MD5Util.encode("${c[Calendar.YEAR]}-${c[Calendar.MONTH] + 1}-${c[Calendar.DAY_OF_MONTH]}")

            val signList = sign.split("\n").map { it.trim().split(" ") }
            signList.forEach { v->
                val entry = zipFile.getEntry(v[0])
                val code = if(entry == null) MD5Util.encode(v[0]) else MD5Util.encode(zipFile.getInputStream(entry))
                fileMd5 = MD5Util.encode(code + fileMd5)
                if(fileMd5 != v[1])
                    throw Exception("${FAIL}:${v[0]}")
            }

            zipFile.close()
            return signList.map { it[0] }
        }
    }

    /**
     * 持久化新的资源版本文件包
     * 如果 id 为空，则为更新主程序的资源文件
     */
    fun saveVersionFile(ver: AppVersion, fis:InputStream): String {
        val id = if(StringUtils.hasText(ver.aid)) {
            if(!StringUtils.hasText(ver.pid))    throw Exception("请指定小程序 ID")
            "${ver.aid}-${ver.pid}"
        }
        else
            EMPTY

        val isMicroPage = id.isNotEmpty()
        val root = Paths.get(config.resHistoryPath, id, "${DateUtil.getDateTimeSimple()}.zip")

        val versionFile = root.toFile()
        logger.debug("$root $versionFile")
        if(!Files.exists(root.parent))
            Files.createDirectories(root.parent)

        FileUtils.copyToFile(fis, versionFile)

        //小程序不做检查
        if(!isMicroPage && config.resZipCheck){
            try{
                checkZipFile(versionFile)
            }
            catch (e:Exception) {
                logger.info("资源包校验失败：{}", e.message)
                FileUtils.deleteQuietly(versionFile)
                throw e
            }
        }

        logger.info("版本文件保存到 $root")

        ver.path = versionFile.path
        ver.size = versionFile.length()
        if(!StringUtils.hasText(ver.version))   ver.version = H.buildVersion()
        ver.addOn = System.currentTimeMillis()

        baseMapper.insert(ver)

        val msg = unzipToDeploy(id, versionFile)

        // 对于小程序，需要注入特定的内容
        if(isMicroPage){
            FileTool.injectText(
                Paths.get(config.resAppPath, id, config.home).toFile(),
                0,
                settingService.value(S.APP_MICRO_INJECT)
            )
        }

        return msg
    }

    /**
     *
     */
    fun unzipToDeploy(id:String, originFile:File): String {
        val targetPath = with(id) {
            var dir = config.resPath
            if(StringUtils.hasText(id))
                dir = "./${config.resAppPath}/${id}"

            logger.info("资源解压到 {}", dir)
            Paths.get(dir)
        }

        FileSystemUtils.deleteRecursively(targetPath)
        return FileTool.unzip(originFile, targetPath).joinToString("<br>")

//        ZipInputStream(FileInputStream(originFile)).use { zipIs->
//            FileSystemUtils.deleteRecursively(targetPath)
//
//            val joiner = StringJoiner("<br>")
//            val targetFolder = targetPath.toFile()
//
//            var entry: ZipEntry? = zipIs.nextEntry
//            while(entry!=null){
//                val file = File(targetFolder, entry.name)
//                if(!file.parentFile.exists())   file.parentFile.mkdirs()
//
//                val msg = "inflating:\t$file"
//                if(logger.isDebugEnabled)   logger.debug(msg)
//                joiner.add(msg)
//
//                if(entry.isDirectory)
//                    file.mkdir()
//                else{
//                    val fileOut = FileOutputStream(file)
//                    fileOut.write(zipIs.readBytes())
//                    fileOut.close()
//                }
//
//                zipIs.closeEntry()
//                entry = zipIs.nextEntry
//            }
//
//            return joiner
//        }
    }
}


@Service
class AppService(
    private val refresh: CacheRefresh,
    private val cfg: AppConfig,
    private val propertyM:AppPropertyMapper) : BaseService<AppMapper, App>() {

    fun detailOf(id:String): Map<String, Any> {
        val app = getById(id)?: throw ServiceException("应用#${id}不存在")

        return mapOf(
            "app"       to app,
            "property"  to propertyM.selectById(id)
        )
    }

    @Transactional
    fun create(model: AppModel) {
        val (app, property) = model

        if(!Regex(cfg.appIdRegex).matches(app.id))
            throw Exception("应用编号[${app.id}]不合规，必须是长度在3-20间的字母数字下划线组合")
        if(count(Q().eq(F.ID, app.id)) > 0)     throw ServiceException("应用编号[${app.id}]已存在")

        app.addOn   = System.currentTimeMillis()
        save(app)

        property.bind(app)
        propertyM.insert(property)
    }

    /**
     * 未来考虑增加数据更新情况
     *
     * 参考 https://blog.csdn.net/Fupengyao/article/details/118599666
     */
    fun update(model: AppModel) {
        val (app, property) = model

        if(!baseMapper.exists(Q().eq(F.ID, app.id)))
            throw Exception("应用编号[${app.id}]不存在")

        updateById(app)

        property.bind(app)
        propertyM.updateById(property)

        refresh.app(app.id)
    }
}