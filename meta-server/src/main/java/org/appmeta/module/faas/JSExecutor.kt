package org.appmeta.module.faas

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONReader
import com.baomidou.mybatisplus.extension.toolkit.SqlRunner
import org.appmeta.Channels
import org.appmeta.domain.DataBlock
import org.appmeta.model.DataCreateModel
import org.appmeta.model.DataDeleteModel
import org.appmeta.model.DataReadModel
import org.appmeta.model.DataUpdateModel
import org.appmeta.module.dbm.DatabaseService
import org.appmeta.module.dbm.DatabaseSourceService
import org.appmeta.module.dbm.DbmModel
import org.appmeta.service.DataService
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.io.IOAccess
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import java.io.OutputStream


/*
 * @project app-meta-server
 * @file    org.appmeta.module.faas.JavaScriptExecutor
 * CREATE   2023年12月27日 11:20 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

interface MetaRuntime {
    val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    fun _log(msg:String, isDebug:Boolean=true){
        if(isDebug)
            if(logger.isDebugEnabled)   logger.debug("[META] $msg")
        else
            logger.info("[META] $msg")
    }

    fun sql(text: String):Any

    fun getBlock(uuid: String):String?
    fun setBlock(uuid: String, text: String)

    /**
     * 新增一条数据行，不指定 pid（如需指定，请使用 insertData(List, pid)
     */
    fun insertData(row: Map<String, Any>) = insertData(listOf(row), DataCreateModel())
    fun insertData(rows: List<Map<String, Any>>, model: DataCreateModel)
    fun updateData(dataId:Long, obj:Map<String, Any>, merge:Boolean)
    fun queryData(model:DataReadModel):Any?
    fun removeData(model: DataDeleteModel)

    fun getSession(uuid: String) = getSession(uuid, null)
    fun getSession(uuid: String, defaultVal:Any?): Any?
    fun setSession(uuid: String, obj:Any?)
}

/**
 * 测试模式下的 JS 环境
 */
class MetaRuntimeDevImpl(val context: FuncContext) : MetaRuntime {
    private fun logToContext(msg: String) = "[DEV-JS] $msg".also {
        context.appendLog(it)
        _log(it)
    }

    override fun sql(text: String): Any = logToContext("执行SQL > $text")

    override fun getBlock(uuid: String): String? {
        logToContext("<BLOCK> 获取数据块 #$uuid （AID=${context.appId}）")
        return uuid
    }

    override fun setBlock(uuid: String, text: String) {
        logToContext("<BLOCK> 更新数据块 #$uuid （AID=${context.appId}）为：$text")
    }

    override fun insertData(rows: List<Map<String, Any>>, model: DataCreateModel) {
        println("新增数据：${JSON.toJSONString(model)}")
        logToContext("<DATA> 新增数据行（PID=${model.pid}） $rows")
    }

    override fun updateData(dataId: Long, obj: Map<String, Any>, merge: Boolean) {
        logToContext("<DATA> 更新数据行 ID=$dataId(MERGE=$merge) > $obj")
    }

    override fun queryData(model: DataReadModel): Any? {
        logToContext("<DATA> 查询数据行 id=${model.id} match=${model.match}")
        return emptyList<Any>()
    }

    override fun removeData(model: DataDeleteModel) {
        logToContext("<DATA> 删除数据行 id=${model.id} match=${model.match}")
    }

    override fun getSession(uuid: String, defaultVal:Any?): Any? {
        logToContext("<SESSION> 获取会话值 #$uuid （默认值=${defaultVal}）")
        return defaultVal
    }

    override fun setSession(uuid: String, obj: Any?) {
        logToContext("<SESSION> 更新会话值 #$uuid 为：${JSON.toJSONString(obj)}")
    }
}

class MetaRuntimeImpl(
    val context: FuncContext,
    val sourceId:Long?,
    val dbService: DatabaseService,
    val dataService: DataService,
    val sessionStore: MutableMap<String, Any?>
):MetaRuntime  {

    override fun sql(text:String):Any {
        if(sourceId == null)    throw Exception("未关联数据源，无法执行SQL")
        if(sourceId == 0L)      return SqlRunner.db().selectList(text)

        val model = DbmModel()
        model.sourceId = sourceId
        model.sql = text

        return dbService.runSQL(model)
    }

    override fun setBlock(uuid:String, text: String) {
        _log("设置(AID=${context.appId}) uuid=$uuid 的 Block...")
        dataService.setBlockTo(DataBlock(context.appId, uuid, text))
    }

    override fun getBlock(uuid: String): String? {
        _log("获取(AID=${context.appId}) uuid=${uuid} 的 Block...")
        return dataService.getBlockBy(DataBlock(context.appId, uuid))?.text
    }

    override fun insertData(rows: List<Map<String, Any>>, model: DataCreateModel) {
        if(StringUtils.hasText(model.batch) && !StringUtils.hasText(model.pid))
            throw Exception("按批次导入数据请指明 pid")

        model.channel = Channels.FAAS
        model.aid = context.appId
        model.uid = context.user.id

        _log("新增数据行 ${rows.size} 条 UID=${model.uid} BATCH=${model.batch}（ORIGIN=${model.origin}）")
        if(logger.isDebugEnabled){
            rows.forEachIndexed { index, d -> _log("数据${index+1} > $d") }
        }
        dataService.create(model)
    }

    override fun updateData(dataId: Long, obj: Map<String, Any>, merge: Boolean) {
        dataService.update(DataUpdateModel().also {
            it.aid = context.appId
            it.id = dataId
            it.merge = merge
            it.obj = obj
        })
        _log("更新数据行 id=$dataId merge=$merge > $obj")
    }

    override fun queryData(model: DataReadModel): Any? {
        model.aid = context.appId
        _log("查询数据行 id=${model.id} pid=${model.pid} match=${model.match}")
        return dataService.read(model)
    }

    override fun removeData(model: DataDeleteModel) {
        model.aid = context.appId
        dataService.delete(model)

        _log("删除数据行 id=${model.id} pid/pids=${model.pid}/${model.pids} match=${model.match}")
    }

    override fun getSession(uuid: String, defaultVal:Any?): Any? {
        _log("获取会话值 ID=$uuid （默认值=${defaultVal}）")
        return sessionStore.getOrDefault(uuid, defaultVal)
    }

    override fun setSession(uuid: String, obj:Any?) {
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
     * 转换为指定的数据对象，支持不加双引号
     */
    private fun <T> parse(v:Any, clazz: Class<T>) = JSON.parseObject(
        v.toString(),
        clazz,
        JSONReader.Feature.AllowUnQuotedFieldNames
    )

    /**
     * 自定义 Java 、JS 互通规则
     *
     * 参数定义规则：https://www.graalvm.org/truffle/javadoc/org/graalvm/polyglot/HostAccess.Builder.html#targetTypeMapping-java.lang.Class-java.lang.Class-java.util.function.Predicate-java.util.function.Function-
     */
    private val hostAccess = HostAccess.newBuilder()
        .allowPublicAccess(true)
        .allowAllImplementations(true)
        .allowAllClassImplementations(true)
        .allowArrayAccess(true)
        .allowListAccess(true)
        .allowBufferAccess(true)
        .allowIterableAccess(true)
        .allowIteratorAccess(true)
        .allowMapAccess(true)
        .allowAccessInheritance(true)
        .also {
            it.targetTypeMapping(Map::class.java, DataReadModel::class.java, { _ -> true}, { v: Map<*, *> -> parse(v, DataReadModel::class.java)} )
            it.targetTypeMapping(Map::class.java, DataCreateModel::class.java, { _ -> true}, { v: Map<*, *> -> parse(v, DataCreateModel::class.java)} )
            it.targetTypeMapping(Map::class.java, DataDeleteModel::class.java, { _ -> true}, { v: Map<*, *> -> parse(v, DataDeleteModel::class.java)} )
        }
        .build()



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
                    val line = String(bytes.subList(cur, bytes.size-1).toByteArray())
                    logger.info("[JS引擎] $line")
                    context.appendLog(line)

                    cur = bytes.size
                }
            }
        }

        val ctx = Context.newBuilder(Func.JS)
            .engine(engine)
            //设置为 HostAccess.ALL 后，可以在 js 中调用 java 方法（通过 Bindings 传递），但是不支持使用 Java.type 功能
            .allowHostAccess(hostAccess)
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
        ctxBindings.putMember(
            "meta",
            if(context.devMode)
                MetaRuntimeDevImpl(context)
            else
                MetaRuntimeImpl(
                    context,
                    func.sourceId,
                    dbService,
                    dataS,
                    sessionData[context.appId]!!
                )
        )

        return ctx.eval(Func.JS, func.cmd).let {
            if(it.isNull)           return null
            if(it.isException)      return it.throwException()
            //转换 JSON 格式
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