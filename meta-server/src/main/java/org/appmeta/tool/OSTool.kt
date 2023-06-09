package org.appmeta.tool

import java.io.File
import java.util.concurrent.TimeUnit


/*
 * @project app-meta-server
 * @file    org.appmeta.tool.OSTool
 * CREATE   2023年03月30日 17:08 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

object OSTool {

    /**
     * 调用系统命令行中的命令.以List<String>的方式输入命令的各个参数.
     * 默认在当前目录中执行,超时时间为60秒
     */
    fun runCommand(
        cmd: List<String>,
        workingDir: File = File("."),
        timeoutAmount: Long = 1L,
        timeUnit: TimeUnit = TimeUnit.SECONDS
    ): Pair<Int, String?> = runCatching {
        val cmds = mutableListOf<String>()
        if(System.getProperty("os.name").uppercase().contains("WINDOW"))
            cmds.addAll(listOf("cmd", "/c"))
        else
            cmds.addAll(listOf("sh", "-c"))

        cmds.addAll(cmd)
        val process = ProcessBuilder(cmd)
            .directory(workingDir)
            .redirectErrorStream(true)
            .start()

        // jdk17之后这样写
        val text = process.also {  it.waitFor(timeoutAmount, timeUnit) }.inputReader().readText()

        Pair(process.exitValue(), text)
    }.onFailure { Pair(-1, it.message) }.getOrThrow()


    /**
     * 调用系统命令
     * 默认在当前目录中执行,超时时间为30秒
     *
     * 返回结果 Pair<进程返回code，文本输出>
     *     code =-1 时为执行报错
     */
    fun runCmd(
        cmd: List<String>,
        workDir: File = File("."),
        timeoutAmount: Long = 20L,
        timeUnit: TimeUnit = TimeUnit.SECONDS
    ):Pair<Int, String?> =
        try {
            val pb = ProcessBuilder(cmd)
                .directory(workDir)
                .redirectErrorStream(true)
                .start()

            // jdk17之后这样写
            val text = pb.also {  it.waitFor(timeoutAmount, timeUnit) }.inputReader().readText()

            Pair(pb.exitValue(), text.trim())
        } catch (e: Exception) {
            Pair(-1, e.message)
        }
}
