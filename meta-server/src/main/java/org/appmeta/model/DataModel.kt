package org.appmeta.model

import org.appmeta.domain.PageWithUser


/*
 * @project app-meta-server
 * @file    org.appmeta.model.DataModel
 * CREATE   2023年02月17日 09:20 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

/*
--------------------------- Data 相关模型 ---------------------------
 */
enum class QueryFilter(val action:String) {
    EQ("="),
    LTE("<="),
    LT("<"),
    GTE(">="),
    GT(">"),
    LIKE("LIKE"),
    NE("!="),
    IN("IN"),
    NIN("NOT IN")
}

class HeaderItem {
    var key         = ""
    var label       = ""
//    var width       = 0
//    var bold        = true

    constructor()
    constructor(f:String, t:String) {
        key         = f
        label       = t
    }
}

class QueryItem {
    var field       = ""
    var op          = QueryFilter.EQ
    var value:Any?  = null

    constructor()
    constructor(field:String, op: QueryFilter, value:Any?){
        this.field  = field
        this.op     = op
        this.value  = value
    }
    constructor(v:Triple<String, QueryFilter, Any?>){
        field       = v.first
        op          = v.second
        value       = v.third
    }
    constructor(sql:String) {
        val tmp     = sql.split(Regex(" "), 3)
        field       = tmp[0]
        op          = QueryFilter.valueOf(tmp[1])
        value       = tmp[2]
    }

    override fun toString() = "$field ${op.action} $value"
}

abstract class DataModel(val action:String = CREATE): PageWithUser() {
    operator fun component1() = aid
    operator fun component2() = pid
    operator fun component3() = uid
    operator fun component4() = obj

    companion object {
        const val CREATE    = "C"
        const val UPDATE    = "U"
        const val READ      = "R"
        const val DELETE    = "D"
    }
//    var action  = CREATE
//        set(value) {  }

    /*
    不考虑值为 null 的情况
     */
    var obj     = mapOf<String, Any>()
    var match   = mutableListOf<QueryItem>()
    // 是否进行批量操作，如果设置此值将给每个数据对象注入批次号 _batch_
    var batch   = ""
    var id      = 0L

    var timeFrom= 0L
    var timeEnd = 0L

    fun of(bean:PageWithUser){
        aid     = bean.aid
        pid     = bean.uid
        uid     = bean.uid
    }
}

class DataCreateModel: DataModel(){
    var objs    = listOf<Map<String, Any>>()
    var origin  = ""
}

class DataUpdateModel: DataModel(UPDATE)

class DataReadModel: DataModel(READ) {
    /**
     * 默认不进行分页，返回 200 条数据
     * 如需分页，请设置 page 大于0
     */
    var page    = 0L
    var pageSize= 200L
    var total   = 0L

    var desc    = false     //是否按照插入时间倒序

    fun toLimit() = if(page<=1) "LIMIT $pageSize" else "LIMIT ${(page-1) * pageSize},${page * pageSize}"

    companion object {
        fun by(id:Long, aid:String): DataReadModel {
            val model   = DataReadModel()
            model.id    = id
            model.aid   = aid
            return model
        }
    }
}

class DataDeleteModel: DataModel(DELETE)

class DataExportModel: DataModel(READ) {
    companion object {
        const val XLSX  = "xlsx"
        const val CSV   = "csv"
    }

    var headers         = mutableListOf<HeaderItem>()
    // 可选 xlsx、csv
    var format          = XLSX
    var sheetName       = "Export"
    var filename        = ""

    /**
     * 返回标题字段列表
     */
    fun headerFields()  = headers.map { it.key }

    fun checkFormat()   = format == XLSX || format == CSV
}