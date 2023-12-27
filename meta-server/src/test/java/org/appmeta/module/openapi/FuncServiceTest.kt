package org.appmeta.module.openapi

import com.alibaba.fastjson2.JSON
import jakarta.annotation.Resource
import org.appmeta.AppTest
import org.junit.jupiter.api.Test
import org.nerve.boot.util.DateUtil


/*
 * @project app-meta-server
 * @file    org.appmeta.module.openapi.ApiServiceTest
 * CREATE   2023年06月07日 16:28 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class FuncServiceTest : AppTest() {

    @Resource
    lateinit var service: ApiService

    private val ID = 1
    private val KEY = "key"

    //[{"id":"key","name":"分组字段","regex":"","required":true,"type":"String"}]
    private fun buildPs() = JSON.toJSONString(
        listOf(
            ApiParmeter(KEY, "分组字段", true)
        )
    )

    @Test
    fun create(){
        service.createOrUpdate(Api().also {
            it.id = 1
            it.name = "示例接口（${DateUtil.getDate()}）"
            it.uid  = UID
        })
    }

    @Test
    fun call(){
        json(service.call(getUser(), 1, mapOf(KEY to "uid")))
    }
}