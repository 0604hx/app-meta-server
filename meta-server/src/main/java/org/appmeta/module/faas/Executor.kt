package org.appmeta.module.faas

import com.baomidou.mybatisplus.extension.toolkit.SqlRunner
import com.github.jknack.handlebars.Handlebars
import org.appmeta.module.dbm.DatabaseService
import org.appmeta.module.dbm.DatabaseSourceService
import org.appmeta.module.dbm.DbmModel
import org.nerve.boot.util.DateUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.util.Assert


/*
 * @project app-meta-server
 * @file    org.appmeta.module.faas.Executor
 * CREATE   2023年12月27日 08:45 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */
/**
 * 函数执行者
 */
interface Executor {
    val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    fun run(func: Func, context: FuncContext): Any?
}

@Component
class SQLExecutor(private val dataSourceS:DatabaseSourceService, private val dbService:DatabaseService):Executor {

    override fun run(func: Func, context: FuncContext): Any? {
        Assert.isTrue(func.sourceId != null, "函数未配置数据源")

        val sql = Handlebars().compileInline(func.cmd).apply(context)
        if(context.devMode){
            context.appendLog("[DEV-SQL] 执行语句 $sql")
            return DateUtil.getDateTime()
        }

        logger.info("执行sql：${sql}")

        return if(func.sourceId == 0L){
            //使用主数据源，只能执行查询
            val items = SqlRunner.db().selectList(sql)

            if(func.resultType == Func.RESULT_ARRAY)
                items.map { it.values }
            else
                items
        }
        else {
            // 指定了 DataBaseSource 时，调用相应的模块
            val source = dataSourceS.withCache(func.sourceId!!)?: throw Exception("数据源#${func.sourceId} 未定义")
            val dbmResult = dbService.runSQL(DbmModel().also {
                it.sourceId = source.id
                it.batch = false
                it.action = DbmModel.SQL
                it.sql = sql
            })

            dbmResult as List<*>
        }
    }
}
