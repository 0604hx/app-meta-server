package org.appmeta.module.faas

import jakarta.annotation.Resource
import org.appmeta.AppTest
import org.junit.jupiter.api.Test


/*
 * @project app-meta-server
 * @file    org.appmeta.module.faas.ExecutorTest
 * CREATE   2023年12月27日 10:54 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class ExecutorTest:AppTest() {

    @Resource
    lateinit var sqlExecutor: SQLExecutor
    @Resource
    lateinit var jsExecutor: JSExecutor

    private fun buildFunc(cmd:String, mode:String=Func.SQL, ps:List<FuncParmeter> = listOf()) = Func().also {
        it.cmd = cmd
        it.mode = mode
        it.params = ps
        it.sourceId = 0L
    }

    @Test
    fun sql(){
        println(
            sqlExecutor.run(
                buildFunc("SELECT count(*) as size, template from page where aid='{{appId}}' and template='{{ params.template }}' and uid='{{ user.id }}'"),
                FuncContext(
                    AID_DEMO,
                    mutableMapOf("template" to "sfc"),
                    UserContext(UID, UNAME)
                )
            )
        )
    }

    @Test
    fun js(){
        val code = """
            console.log(`调用JS脚本，参数`, params, "用户ID", user.id)
            console.debug(`即将返回当前时间戳...`)
            
            let appList = meta.sql(params.sql)
            console.debug(appList[0])
            
            const ID = "js-time"
            //获取 Block
            console.debug(`获取ID=time的数据块：`, meta.getBlock(ID))
            //meta.setBlock(ID, Date().toString())
            
            meta.setSession(ID, ["ABC", "DEF"])
            console.debug(`获取ID=time的会话值：`, meta.getSession(ID));
            
            [{ time: Date.now() }]
        """.trimIndent()
        println(
            jsExecutor.run(
                buildFunc(code, Func.JS),
                FuncContext(
                    AID_DEMO,
                    mutableMapOf(
                        "name"  to "集成显卡",
                        "sql"   to "SELECT id,name FROM app"
                    ),
                    UserContext(UID, UNAME)
                )
            )
        )
    }
}