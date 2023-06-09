package org.appmeta.web.system

import jakarta.annotation.Resource
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.input.ReversedLinesFileReader
import org.appmeta.AppMetaServer
import org.appmeta.H
import org.appmeta.Role
import org.appmeta.S
import org.appmeta.component.SystemConfig
import org.appmeta.model.SizeModel
import org.nerve.boot.Const.COMMA
import org.nerve.boot.domain.AuthUser
import org.nerve.boot.module.operation.Operation
import org.nerve.boot.module.setting.SettingService
import org.nerve.boot.util.DateUtil
import org.nerve.boot.web.ctrl.BasicController
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.util.Assert
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedInputStream
import java.io.BufferedWriter
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption


/*
 * @project app-meta-server
 * @file    org.appmeta.web.system.SystemCtrl
 * CREATE   2023年05月23日 17:16 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

open class BasicSystemCtrl:BasicController() {
    @Resource
    protected lateinit var settingS:SettingService

    fun adminAndWhiteIP(): AuthUser {
        val user = authHolder.get()
        val whiteIps = settingS.value(S.SYS_WHITE_IP)?.split(COMMA)?: emptyList()
        if(H.hasAnyRole(user, Role.ADMIN, Role.SYS_ADMIN) && whiteIps.contains(requestIP))
            return user

        throw Exception("该功能需要 ${Role.ADMIN}/${Role.SYS_ADMIN} 在特定设备下执行")
    }
}

@Service
class SystemHelper {
    val logger = LoggerFactory.getLogger(javaClass)

    @Async
    fun restart() = AppMetaServer.restart()
}

@RestController
@RequestMapping("system")
class SystemCtrl(private val helper: SystemHelper, private val sysConfig:SystemConfig) : BasicSystemCtrl() {

    @RequestMapping("restart", name = "重启后端")
    fun restart() = result {
        val user = adminAndWhiteIP()
        opLog("${user.showName} 在 $requestIP 申请重启后端服务...", null, Operation.MODIFY)

        helper.restart()
    }

    @PostMapping("update-jar", name = "更新主程序 JAR")
    fun updateJAR(@RequestPart("file") file: MultipartFile) = result { re->
        Assert.isTrue(sysConfig.enableJar, "未开启更新 JAR 功能")

        val user = adminAndWhiteIP()
        opLog("${user.showName} 在 $requestIP 上传 ${file.originalFilename}(size=${file.size}) 以更新后端 JAR...", null, Operation.MODIFY)

        val ext = FilenameUtils.getExtension(file.originalFilename)
        Assert.isTrue(ext.uppercase() == "JAR", "文件格式不支持")

        val jarFile = Paths.get(sysConfig.jarName).also {
            if(Files.exists(it)){
                val bkFile = Paths.get("${sysConfig.jarName}.bk")
                Files.copy(it, bkFile, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING)
                logger.info("复制当前 jar 到 $bkFile")
            }
        }

        /*
        经过多次试验，发现通过 FileOutputStream 的方式可以正常写入内容

        方式一：
        FileUtils.copyToFile(file.inputStream, jarFile.toFile())

        方式二：
        Files.copy(file.inputStream, newFile, StandardCopyOption.REPLACE_EXISTING)
         */
//        RandomAccessFile(jarFile.toFile(), "rws").use { f->
//            val fis = BufferedInputStream(file.inputStream)
//            val buffer = ByteArray(10*1024*1024)
//            var len: Int
//            val total = file.size
//            var current = 0
//
//            while (
//                run {
//                    len = fis.read(buffer)
//                    len
//                } != -1
//            ) {
//                current += len
//                logger.info("[COPY] %-10d / %-10d (READ %-8d)".format(current, total, len))
//                f.write(buffer, 0, len)
//            }
//
//            f.channel.close()
//            /*
//            写入到更新日志文件中
//             */
//            FileOutputStream(sysConfig.verLogOfJar, true).use { logFOS->
//                BufferedWriter(OutputStreamWriter(logFOS, Charsets.UTF_8)).use { bw->
//                    bw.newLine()
//
//                    val line = "%s %-8s %s".format(DateUtil.getDateTime(), user.id, "${file.size/1024}KB")
//                    bw.write(line)
//
//                    logger.info("追加到更新日志 ${sysConfig.verLogOfJar}：$line")
////                    re.data = line
//                }
//            }
//
//            logger.info("${user.showName} 更新 JAR 包到 ${sysConfig.jarName}")
//        }
        FileOutputStream(jarFile.toFile()).use { f->
            val fis = BufferedInputStream(file.inputStream)
            val buffer = ByteArray(10*1024*1024)
            var len: Int
            val total = file.size
            var current = 0

            while (
                run {
                    len = fis.read(buffer)
                    len
                } != -1
            ) {
                current += len
                logger.info("[COPY] %-10d / %-10d (READ %-8d)".format(current, total, len))
                f.write(buffer, 0, len)
            }
            f.flush()
            fis.close()

            /*
            写入到更新日志文件中
             */
            FileOutputStream(sysConfig.verLogOfJar, true).use { logFOS->
                BufferedWriter(OutputStreamWriter(logFOS, Charsets.UTF_8)).use { bw->
                    bw.newLine()

                    val line = "%s %-8s %s".format(DateUtil.getDateTime(), user.id, "${file.size/1024} KB")
                    bw.write(line)

                    logger.info("追加到更新日志 ${sysConfig.verLogOfJar}：$line")
                    re.data = line
                }
            }
            logger.info("${user.showName} 更新 JAR 包 ${sysConfig.jarName}")
        }
    }

    @PostMapping("log", name = "下载最新的日志文件")
    fun downloadLog(response: HttpServletResponse) {
        val user = adminAndWhiteIP()
        val logFile = Paths.get(sysConfig.logFile)
        if(Files.notExists(logFile))
            throw Exception("更新记录文件 ${sysConfig.logFile} 不存在")
        else
            downloadFile(response, logFile.toFile(), "meta-server-${DateUtil.getDateTimeSimple()}.log") {
                opLog("${user.showName} 在 $requestIP 下载运行时日志", null, Operation.EXPORT)
            }
    }

    @PostMapping("log-version", name = "查看最近的更细记录")
    fun versionLog(@RequestBody model: SizeModel) = resultWithData {
        adminAndWhiteIP()

        val p = Paths.get(sysConfig.verLogOfJar)
        if(Files.exists(p))
            ReversedLinesFileReader(p, Charsets.UTF_8).use { it.readLines(model.size) }
        else
            throw Exception("版本记录文件 ${sysConfig.verLogOfJar} 不存在")
    }
}