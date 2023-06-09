package org.appmeta.module.openapi

import com.baomidou.mybatisplus.annotation.TableName
import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Select
import org.appmeta.Caches
import org.appmeta.domain.Authable
import org.appmeta.domain.Launchable
import org.nerve.boot.annotation.CN
import org.nerve.boot.domain.IDLong
import org.springframework.cache.annotation.Cacheable

/*
 * @project app-meta-server
 * @file    org.appmeta.module.openapi.Domain
 * CREATE   2023年06月07日 15:42 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class ApiParmeter {
    var id      = ""
    var name    = ""
    var value:String?   = null
    var required= false
    var regex   = ""
    var type    = "String"

    constructor()
    constructor(id: String, name:String, required:Boolean = false, regex:String="") {
        this.id = id
        this.name = name
        this.required = required
        this.regex = regex
    }
}

@CN("开放接口")
@TableName("api")
class Api:IDLong(), Launchable {
    var pid:Long?   = null
    var uid         = ""
    var name        = ""
    override var launch         = 0

    override fun toString() = "开放接口⌈${id}/${name}⌋"
}

@CN("开放接口详细")
@TableName("api_detail")
class ApiDetail:IDLong, Authable {
    companion object {
        const val ARRAY     = "Array"
        const val OBJECT    = "Object"
    }

    var active      = false
    var summary     = ""
    var params      = ""
    var sourceId:Long? = null
    var cmd         = ""
    var resultType  = OBJECT
    var addOn       = 0L

    override var serviceAuth    = ""
    override var editAuth       = ""
    override var uid            = ""

    constructor()
    constructor(api:Api){
        this.setId(api.id)
        uid = api.uid
        summary = "> ${api.name}"
        addOn = System.currentTimeMillis()
    }
}

@Mapper
interface ApiMapper:BaseMapper<Api> {
    @Cacheable(Caches.API)
    @Select("SELECT * FROM api WHERE id=#{0}")
    fun withCache(id: Long): Api?
}

@Mapper
interface ApiDetailMapper:BaseMapper<ApiDetail> {
    @Cacheable(Caches.API_DETAIL)
    @Select("SELECT * FROM api_detail WHERE id=#{0}")
    fun withCache(id: Long): ApiDetail?
}