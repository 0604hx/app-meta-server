package org.appmeta.service

import jakarta.annotation.Resource
import org.appmeta.AppTest
import org.appmeta.model.QueryModel
import org.junit.jupiter.api.Test


/*
 * @project app-meta-server
 * @file    org.appmeta.service.PageServiceTest
 * CREATE   2023年03月21日 09:19 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class PageServiceTest:AppTest() {
    @Resource
    lateinit var service: PageService

    @Test
    fun list(){
        val model = QueryModel()
        model.form = mutableMapOf("EQ_aid" to "FKZX_CLSCQ")

        println("筛选页面数量=${service.list(model).size}, HASH=${model.hashCode()}")
        println("筛选页面数量=${service.list(model).size}, HASH=${model.hashCode()}")

        model.fields = listOf("name", "template")
        println("筛选页面数量=${service.list(model).size}, HASH=${model.hashCode()}")

        val model2 = QueryModel()
        model2.form = mutableMapOf("EQ_aid" to "FKZX_CLSCQ")
        println("筛选页面数量=${service.list(model).size}, HASH=${model.hashCode()}")
        println("筛选页面数量=${service.list(model2).size}, HASH=${model2.hashCode()}")
    }
}