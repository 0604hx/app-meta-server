package org.appmeta.web.page

import com.alibaba.fastjson2.JSON
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import org.appmeta.ALL
import org.appmeta.F.ID
import org.appmeta.F.TEMPLATE
import org.appmeta.domain.Page
import org.appmeta.domain.Page.Companion.FAAS
import org.appmeta.domain.PageMapper
import org.appmeta.module.faas.FaasService
import org.appmeta.module.faas.Func
import org.appmeta.module.faas.FuncContext
import org.appmeta.module.faas.UserContext
import org.appmeta.service.AccountHelper
import org.appmeta.service.AppRoleService
import org.appmeta.service.PageService
import org.appmeta.tool.AuthHelper
import org.appmeta.web.AnonymousAbleCtrl
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


/*
 * @project app-meta-server
 * @file    org.appmeta.web.page.SupportFaasCtrl
 * CREATE   2023年12月27日 16:57 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@RestController
@RequestMapping("faas")
class SupportFaasCtrl(
    private val pageS: PageService,
    private val appRoleS: AppRoleService,
    private val authHelper: AuthHelper,
    private val accountHelper: AccountHelper,
    private val service:FaasService
) : AnonymousAbleCtrl() {

    private fun _error(msg:String, page:Page):Void = throw Exception("$msg，请联系管理者<${page.uid}>")

    @RequestMapping("{id}")
    fun faas(@PathVariable id:Long) = resultWithData {
        if(logger.isDebugEnabled){
            logger.debug("请求调用 FaaS 函数#${id}")
            val params = request.parameterNames
            while (params.hasMoreElements()) {
                val key = params.nextElement()
                logger.debug("[PARAMS] {} = {}", key, request.getParameterValues(key))
            }
        }
        val page = pageS.getOne(QueryWrapper<Page>().eq(ID, id).eq(TEMPLATE, FAAS))?: throw Exception("FaaS函数 #$id 不存在")
        if(!page.active)    _error("功能未开放", page)

        val user = getUserOrNull()
        val canCall = if(page.serviceAuth == ALL){
            logger.info("访问完全公开的 FaaS 函数#${id}")
            true
        }
        else {
            authHelper.checkService(page, user)
        }

        if(!canCall)        _error("您未授权访问该功能", page)

        if(logger.isDebugEnabled)   logger.debug("FaaS 函数开始执行，用户=${user?.showName}")
        service.execute(
            JSON.parseObject(pageS.buildContent(page, false), Func::class.java),
            FuncContext(
                page.aid,
                request.parameterNames.toList().let {
                    val ps = mutableMapOf<String, Any>()
                    it.forEach { key-> ps[key] = request.getParameter(key) }
                    ps
                },
                if(user == null)
                    UserContext.empty()
                else
                    UserContext(user).also {
                        val cache = appRoleS.loadRoleAndAuthOfUser(page.aid, user.ip)
                        it.appRoles = cache.first
                        it.appAuths = cache.second
                    }
            )
        ).also {
            if(logger.isDebugEnabled)   logger.debug("FaaS 函数执行完成: {}", it)
        }
    }
}