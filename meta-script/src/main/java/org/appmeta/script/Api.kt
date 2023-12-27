package org.appmeta.script


/*
 * @project app-meta-server
 * @file    org.appmeta.domain.Api
 * CREATE   2023年12月25日 17:16 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class ApiParmeter {
    companion object {
        const val STRING = "string"
        const val NUMBER = "number"
        const val BOOLEAN= "boolean"
    }

    var id              = ""
    var name            = ""
    var value: String?  = null
    var required        = false
    var regex           = ""
    var type            = STRING

    constructor()
    constructor(id: String, name: String, required: Boolean = false, regex: String = "") {
        this.id = id
        this.name = name
        this.required = required
        this.regex = regex
    }
}

class Api {
    companion object {
        const val SQL               = "sql"
        const val JS                = "js"

        const val ARRAY             = "Array"
        const val OBJECT            = "Object"
    }

    var mode                        = SQL
    var summary                     = ""
    var params:List<ApiParmeter>    = listOf()  //入参配置
    var sourceId:Long?              = null      //数据源ID
    var cmd                         = ""        //代码或者脚本
    var resultType                  = OBJECT    //结果格式（针对 mode=sql）
}