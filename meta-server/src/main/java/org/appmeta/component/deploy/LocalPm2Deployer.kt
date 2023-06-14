package org.appmeta.component.deploy

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.appmeta.F
import org.appmeta.IS_WINDOW
import org.appmeta.component.AppConfig
import org.appmeta.domain.Terminal
import org.appmeta.domain.Terminal.Companion.LANG_JAVA
import org.appmeta.domain.Terminal.Companion.LANG_NODE
import org.appmeta.tool.FileTool
import org.appmeta.tool.OSTool
import org.nerve.boot.Const.EMPTY
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.util.FileCopyUtils
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.notExists

@Component
class LocalPm2Deployer(private val config: AppConfig):Deployer {

    val logger = LoggerFactory.getLogger(javaClass)

    val CMD = if(IS_WINDOW) "pm2.cmd" else "pm2"

    override fun name() = "[LOCAL-PM2-DEPLOYER]"

    private fun callPM2(vararg cmd:String): Pair<Int, String?> {
        val pm = OSTool.runCmd(listOf(CMD, * cmd))
        if(logger.isDebugEnabled)   logger.debug("${name()} 执行命令「 ${cmd.joinToString(" ")}」  CODE=${pm.first} MSG=${pm.second}")
        if(pm.first != 0) throw Exception("${name()} 执行出错 ${pm.second}")
        return pm
    }

    override fun checkRequirement(): Boolean {
        val node = OSTool.runCmd(listOf("node", "-v"))
        if(logger.isDebugEnabled)   logger.debug("${name()} 检测 node 环境：${node.second}")

        val pm2 = OSTool.runCmd(listOf(CMD, "-v"))
        if(logger.isDebugEnabled)   logger.debug("${name()} 检测 pm2 环境：${pm2.second}")

        if(node.first == 0 && pm2.first == 0)
            return true

        throw Exception("${name()} 请先安装 node 及 pm2")
    }

    override fun deploy(id:String, codeFile: File, terminal: Terminal) {
        if(terminal.mode == Terminal.OUTSIDE)   throw Exception("应用「${id}」部署方式需设置为 ${Terminal.INSIDE}")

        val dir = Paths.get(config.terminalPath, id)
        if(Files.notExists(dir))
            Files.createDirectories(dir)

        if(codeFile.extension.uppercase() == "ZIP") {
            // 如果是压缩文件，则解压到 dir 目录下
            FileTool.unzip(codeFile, dir, FileTool.UNZIP_SKIP_ON_EXIST)
        }
        else{
            val entryFile = dir.resolve(codeFile.name).toFile()
            FileCopyUtils.copy(codeFile, entryFile)
            if(logger.isDebugEnabled)   logger.debug("${name()} 复制文件到 $entryFile")
        }

//        val stop = OSTool.runCmd(listOf(CMD, "delete", id))
//        if(stop.first == 0) logger.info("${name()} 停止并移除应用「$id」的服务")

        //写入配置文件
        val configFile = dir.resolve(config.terminalConfig)
        FileOutputStream(configFile.toFile()).use {
            IOUtils.write(JSON.toJSONString(terminal), it, Charsets.UTF_8)
            logger.info("${name()} 写入配置文件 ${config.terminalConfig}")
        }
        //创建 pm2 的 config.js
        val startFile = dir.resolve(config.terminalStart)
        if(startFile.notExists()){
            logger.info("${name()} 检测到启动文件（${config.terminalStart})不存在，即将自动创建...")

            FileOutputStream(startFile.toFile()).use {
                val script = when(terminal.language){
                    LANG_NODE   -> codeFile.name
                    LANG_JAVA   -> LANG_JAVA
                    else        -> throw Exception("未实现的开发语言⌈${terminal.language}⌋")
                }

                IOUtils.write(
                    """
                    module.exports = {
                    	apps:[{
                    		name: "$id(${terminal.port})",
                    		script:"$script",
                    		exec_interpreter:"${if(terminal.language == LANG_NODE) LANG_NODE else EMPTY}",
                    		args:"${terminal.args}"
                    	}]
                    }""".trimIndent(),
                    it,
                    Charsets.UTF_8
                )
            }
        }
        val cmds = mutableListOf(CMD, "restart", config.terminalStart)

//        val cmds = mutableListOf(
//            CMD,
//            "start",
//            codeFile.name,
//            "--name=${id}"
//        )
//        if(StringUtils.hasText(terminal.args)) {
//            cmds.add("--")
//            cmds.addAll(terminal.args.split(" "))
//        }

        val pm2 = OSTool.runCmd(cmds, dir.toFile())
        if(pm2.first != 0)  throw Exception("${name()} 启动应用「$id」服务出错：${pm2.second}")
    }

    override fun restart(id: String) = callPM2("restart", id).first == 0

    override fun stop(id: String) = callPM2("stop", id).first == 0

    override fun remove(id: String) {
        callPM2("delete", id)

        //删除文件
        val dir = Paths.get(config.terminalPath, id)
        FileUtils.deleteDirectory(dir.toFile())
        logger.info("${name()} 删除应用服务（$dir）")
    }

    /**
     *     var pid         = 0         //进程ID
     *     var name        = ""
     *     var mem         = 0L        //内存，b
     *     var cpu         = 0.0F      //cpu 占用
     *     var version     = ""        //应用版本
     *     var vmVersion   = ""        //容器/虚拟机版本
     *     var uptime      = 0L        //启动时间戳
     *     var status      = ""
     *     var addOn       = 0L        //统计时间
     */
    override fun overview():List<TerminalProcess> {
        //在 windows 下，执行 jList 会阻塞，故进行 5 秒限定
        val pm2 = OSTool.runCmd(listOf(CMD, "jlist"), File("."), if(IS_WINDOW) 2 else 5)
        if(pm2.first != 0)  throw Exception("${name()} 执行出错 ${pm2.second}")

        val obj = JSON.parseArray(pm2.second, JSONObject::class.java)
        return obj.map { json->
            with(TerminalProcess()){
                pid     = json.getIntValue("pid", pid)
                name    = json.getString(F.NAME)

                val monit = json.getJSONObject("monit")
                if(monit != null){
                    mem = monit.getLong("memory")
                    cpu = monit.getFloat("cpu")
                }

                val env = json.getJSONObject("pm2_env")
                if(env != null){
                    vm  = env.getString("exec_interpreter").uppercase()
                    if(vm == "NONE"){
                        //判断
                        vm =  FilenameUtils.getBaseName(env.getString("pm_exec_path"))
                    }
                    version = env.getString("version")
                    vmVersion = env.getString("node_version")?: ""
                    uptime = env.getLong("pm_uptime")
                    status = env.getString("status")
                }

                this
            }
        }
    }
}