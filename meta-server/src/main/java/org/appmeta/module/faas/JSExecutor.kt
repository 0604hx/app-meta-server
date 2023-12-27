package org.appmeta.module.faas

import com.alibaba.fastjson2.JSON
import com.baomidou.mybatisplus.extension.toolkit.SqlRunner
import org.appmeta.domain.DataBlock
import org.appmeta.module.dbm.DatabaseService
import org.appmeta.module.dbm.DatabaseSourceService
import org.appmeta.module.dbm.DbmModel
import org.appmeta.service.DataService
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.io.IOAccess
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.OutputStream


/*
 * @project app-meta-server
 * @file    org.appmeta.module.faas.JavaScriptExecutor
 * CREATE   2023年12月27日 11:20 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class MetaRuntime(
    val appId:String,
    val sourceId:Long?,
    val dbService: DatabaseService,
    val dataService: DataService,
    val sessionStore: MutableMap<String, Any?>
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private fun _log(msg:String, isDebug:Boolean=true){
        if(isDebug)
            if(logger.isDebugEnabled)   logger.debug("[META] $msg")
        else
            logger.info("[META] $msg")
    }

    fun sql(text:String):Any {
        if(sourceId == null)    throw Exception("未关联数据源，无法执行SQL")
        if(sourceId == 0L)      return SqlRunner.db().selectList(text)

        val model = DbmModel()
        model.sourceId = sourceId
        model.sql = text

        return dbService.runSQL(model)
    }

    fun setBlock(uuid:String, text: String) {
        _log("设置(AID=${appId}) uuid=$uuid 的 Block...")
        dataService.setBlockTo(DataBlock(appId, uuid, text))
    }

    fun getBlock(uuid: String): String? {
        _log("获取(AID=${appId}) uuid=${uuid} 的 Block...")
        return dataService.getBlockBy(DataBlock(appId, uuid))?.text
    }

    fun getSession(uuid: String): Any? {
        _log("获取会话值 ID=$uuid")
        return sessionStore.get(uuid)
    }

    fun setSession(uuid: String, obj:Any?) {
        _log("设置会话值 $uuid = $obj")
        sessionStore[uuid] = obj
    }
}

@Component
class JSExecutor(
    private val dataS: DataService,
    private val dataSourceS: DatabaseSourceService,
    private val dbService: DatabaseService):Executor {

    private val engine: Engine = Engine.newBuilder().option("engine.WarnInterpreterOnly", "false").build()

    /**
     * 目前会话级别的数据存储，平台重启后失效
     */
    private val sessionData = mutableMapOf<String, MutableMap<String, Any?>>()

    override fun run(func: Func, context: FuncContext): Any? {
        func.sourceId?.also {
            if(it>0L)
                dataSourceS.withCache(it)?: throw Exception("数据源#${func.sourceId} 未定义")
        }

        if(!sessionData.contains(context.appId))
            sessionData[context.appId] = mutableMapOf()

        val out = object : OutputStream (){
            val bytes = mutableListOf<Byte>()
            private var cur = 0

            override fun write(b: Int) {
                bytes.add(b.toByte())

                if(b==10){
                    logger.info("[JS引擎] ${String(bytes.subList(cur, bytes.size-1).toByteArray())}")
                    cur = bytes.size
                }
            }
        }

        val ctx = Context.newBuilder(Func.JS)
            .engine(engine)
            //设置为 HostAccess.ALL 后，可以在 js 中调用 java 方法（通过 Bindings 传递），但是不支持使用 Java.type 功能
            .allowHostAccess(HostAccess.ALL)
            //设置 JS 与 JAVA 的交互性（如 Java.type、Packages ）
            //.allowAllAccess(true)
            //不允许IO（如引入外部文件）
            .allowIO(IOAccess.NONE)
            .out(out)
            .build()


        val ctxBindings = ctx.getBindings(Func.JS)
        ctxBindings.putMember("params", context.params)
        ctxBindings.putMember("user", context.user.toMap())
        ctxBindings.putMember("appId", context.appId)
        ctxBindings.putMember("meta", MetaRuntime(
            context.appId,
            func.sourceId,
            dbService,
            dataS,
            sessionData[context.appId]!!
        ))

        return ctx.eval(Func.JS, func.cmd).let {
            JSON.parse(it.toString())
//            if(it.isNull)           return null
//            if(it.isString)         return it.asString()
//            if(it.isHostObject)     return it.asHostObject()
//            if(it.isBoolean)        return it.asBoolean()
//            if(it.isDate)           return it.asDate()
//            if(it.fitsInInt())      return it.asInt()
//            if(it.fitsInLong())     return it.asLong()
//            if(it.fitsInShort())    return it.asShort()
//            if(it.fitsInDouble())   return it.asDouble()
//            if(it.fitsInByte())     return it.asByte()
//            if(it.isException)      return it.throwException()
//
//            return it.asString()
        }
    }
}