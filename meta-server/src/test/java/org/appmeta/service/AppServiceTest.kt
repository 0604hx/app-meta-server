package org.appmeta.service

import jakarta.annotation.Resource
import org.appmeta.AppTest
import org.appmeta.domain.AppLink
import org.appmeta.domain.AppMapper
import org.appmeta.domain.Page
import org.appmeta.domain.PageMapper
import org.junit.jupiter.api.Test

class AppServiceTest:AppTest() {

    @Resource
    lateinit var appRoleS:AppRoleService
    @Resource
    lateinit var appLinkS:AppLinkService
    @Resource
    lateinit var pageM:PageMapper
    @Resource
    lateinit var mapper: AppMapper

    @Test
    fun checkRole(){
        val isAdmin = appRoleS.isAdmin(AID, UID)
        json(isAdmin)
    }

    @Test
    fun createLink(){
        try{
            appLinkS.create(AppLink(AID, UID))
        }
        catch (e:Exception){
            logger.info("无法创建关联：${e.message}")
        }

        json(appLinkS.byUid(UID, AppLink.MARK))
    }

    @Test
    fun pageList(){
        json(pageM.selectList(null))
        json(pageM.getContent(1))
    }

    @Test
    fun pageCreate(){
        with(Page("SJCQ-BDB", UID)){
            content = "{}"
            main    = true
            active  = true
            name    = "数据填报"

            pageM.insert(this)
        }
    }

    @Test
    fun withCache(){
        json(mapper.withCache("SJCQ-BDB"))
    }
}