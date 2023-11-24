package org.appmeta.module.pty

import com.pty4j.PtyProcess
import com.pty4j.PtyProcessBuilder
import org.appmeta.IS_WINDOW
import org.junit.jupiter.api.Test


/*
 * @project app-meta-server
 * @file    org.appmeta.module.pty.PtyTest
 * CREATE   2023年11月24日 17:17 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class PtyTest {

    lateinit var process:PtyProcess
    lateinit var readThread: Thread

    private fun close(){
        process.destroyForcibly()
        process.waitFor()
    }

//    private fun init(){
//
//    }
    private val readRunnable = Runnable {
        try {
            process.inputStream.use { inputStream ->
                var i = 0
                val buffer = ByteArray(2048)
                println("开始读取输出...")
                while (inputStream.read(buffer).also { i = it } != -1) {
                    val msg = String(buffer, 0, i)
                    println(msg)
                }
            }
        } catch (e: Exception) {
            println("读取出错：${e.message}")
        } finally {
            println("--------------------- EXIT ---------------------")
        }
    }

    @Test
    fun pty(){
        PtyProcessBuilder()
            .setCommand(if(IS_WINDOW) arrayOf("cmd") else arrayOf("/bin/sh", "-l"))
            .setEnvironment( LinkedHashMap<String, String>(System.getenv()).also { it["TERM"] = "xterm"} )
            .setWindowsAnsiColorEnabled(true)
            .setInitialColumns(80)
            .setInitialRows(120)
            .start().also { p ->
                process = p
                val outS = p.outputStream

                readThread = Thread(readRunnable).also { t->
                    t.setDaemon(true)
                    t.start()
                }

                outS.write("java -version\r\n".toByteArray())
                outS.flush()

                Thread.sleep(10*1000)
            }
    }
}