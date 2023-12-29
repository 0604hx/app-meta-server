package org.appmeta.web.page

import com.alibaba.fastjson2.JSON
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import org.appmeta.ALL
import org.appmeta.F.ID
import org.appmeta.F.TEMPLATE
import org.appmeta.domain.Page
import org.appmeta.domain.Page.Companion.FAAS
import org.appmeta.domain.TerminalLog
import org.appmeta.domain.TerminalLogMapper
import org.appmeta.module.faas.FaasService
import org.appmeta.module.faas.Func
import org.appmeta.module.faas.FuncContext
import org.appmeta.module.faas.UserContext
import org.appmeta.service.AccountHelper
import org.appmeta.service.AppRoleService
import org.appmeta.service.LogAsync
import org.appmeta.service.PageService
import org.appmeta.tool.AuthHelper
import org.appmeta.web.AnonymousAbleCtrl
import org.nerve.boot.Const.EMPTY
import org.nerve.boot.Const.NEW_LINE
import org.nerve.boot.util.Timing
import org.springframework.http.HttpHeaders
import org.springframework.scheduling.annotation.Async
import org.springframework.util.StreamUtils
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
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
    private val service:FaasService,
    private val logAsync: LogAsync
) : AnonymousAbleCtrl() {

    private fun _error(msg:String, page:Page):Void = throw Exception("$msg，请联系管理者<${page.uid}>")

    /**
     * 调用日志记录到 TerminalLog
     * host 为功能 ID
     * code 恒定为 -1
     * url  为参数
     * summary 为函数输出的日志+报错信息
     */
    @RequestMapping("{id}")
    fun faas(@PathVariable id:Long, @RequestParam(required = false) devMode: Boolean = false) = resultWithData {
        val contentType = request.getHeader(HttpHeaders.CONTENT_TYPE)?: EMPTY

        if(logger.isDebugEnabled){
            logger.debug("请求调用 FaaS 函数#${id} CONTENT_TYPE=${contentType} DEV_MODE=${devMode}")
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

        //获取原始请求参数，兼容 json 及传统表单
        val originParams = if(contentType.lowercase().startsWith("application/json")){
            val jsonBody = String(StreamUtils.copyToByteArray(request.inputStream), Charsets.UTF_8)
            if(StringUtils.hasText(jsonBody))
                JSON.parseObject(jsonBody)
            else
                mutableMapOf()
        }
        else{
            reqQueryParams
            //                request.parameterNames.toList().let {
            //                    it.forEach { key-> ps[key] = request.getParameter(key) }
            //                    ps
            //                }
        }

        if(logger.isDebugEnabled)   logger.debug("原始入参：${originParams}")
        val log = TerminalLog(page.aid, "$id", EMPTY)
        val timing = Timing()

        FuncContext(
            page.aid,
            originParams,
            if(user == null)
                UserContext.empty()
            else
                UserContext(user).also {
                    val cache = appRoleS.loadRoleAndAuthOfUser(page.aid, user.ip)
                    it.appRoles = cache.first
                    it.appAuths = cache.second
                },
            devMode
        ).let { context->
            context.appendLog("[参数] ${JSON.toJSONString(originParams)}")
            service.execute( JSON.parseObject(pageS.buildContent(page, false), Func::class.java), context ).let { funResult->
                if(logger.isDebugEnabled)   logger.debug("FaaS 函数执行完成（DEV_MODEL=${devMode}）: {}", funResult)

                if(!devMode){
                    log.channel     = getChannel()
                    log.method      = request.method
                    log.uid         = if(user == null) EMPTY else user.id
                    log.used        = timing.toMillSecond()
                    log.summary     = context.logs.joinToString(NEW_LINE)
                    logAsync.save(log)

                    funResult
                }
                else{
                    //使用 DEV 模式运行 FaaS，直接返回上下文对象
                    context.result = funResult
                    context
                }
            }
        }
    }


//    private fun runFaas(id: Long, devMode:Boolean = false):Any? {
//        val contentType = request.getHeader(HttpHeaders.CONTENT_TYPE)?: EMPTY
//        if(logger.isDebugEnabled){
//            logger.debug("请求调用 FaaS 函数#${id} CONTENT_TYPE=${contentType}")
//            val params = request.parameterNames
//            while (params.hasMoreElements()) {
//                val key = params.nextElement()
//                logger.debug("[PARAMS] {} = {}", key, request.getParameterValues(key))
//            }
//        }
//        val page = pageS.getOne(QueryWrapper<Page>().eq(ID, id).eq(TEMPLATE, FAAS))?: throw Exception("FaaS函数 #$id 不存在")
//        if(!page.active)    _error("功能未开放", page)
//
//        val user = getUserOrNull()
//        val canCall = if(page.serviceAuth == ALL){
//            logger.info("访问完全公开的 FaaS 函数#${id}")
//            true
//        }
//        else {
//            authHelper.checkService(page, user)
//        }
//
//        if(!canCall)        _error("您未授权访问该功能", page)
//
//        if(logger.isDebugEnabled)   logger.debug("FaaS 函数开始执行，用户=${user?.showName}")
//
//        //request.getHeader(HttpHeaders.CONTENT_TYPE) ==
//        val originParams = if(contentType.lowercase().startsWith("application/json")){
//            val jsonBody = String(StreamUtils.copyToByteArray(request.inputStream), Charsets.UTF_8)
//            if(StringUtils.hasText(jsonBody))
//                JSON.parseObject(jsonBody)
//            else
//                mutableMapOf()
//        }
//        else{
//            reqQueryParams
//            //                request.parameterNames.toList().let {
//            //                    it.forEach { key-> ps[key] = request.getParameter(key) }
//            //                    ps
//            //                }
//        }
//        if(logger.isDebugEnabled)   logger.debug("原始入参：${originParams}")
//        val log = TerminalLog(page.aid, "$id", EMPTY)
//        val timing = Timing()
//
//        return FuncContext(
//            page.aid,
//            originParams,
//            if(user == null)
//                UserContext.empty()
//            else
//                UserContext(user).also {
//                    val cache = appRoleS.loadRoleAndAuthOfUser(page.aid, user.ip)
//                    it.appRoles = cache.first
//                    it.appAuths = cache.second
//                },
//            devMode
//        ).let { context->
//            context.appendLog("[参数] ${JSON.toJSONString(originParams)}")
//
//            service.execute( JSON.parseObject(pageS.buildContent(page, false), Func::class.java), context ).let { funResult->
//                if(logger.isDebugEnabled)   logger.debug("FaaS 函数执行完成（DEV_MODEL=${devMode}）: {}", funResult)
//
//                if(!devMode){
//                    log.channel     = getChannel()
//                    log.method      = request.method
//                    log.uid         = if(user == null) EMPTY else user.id
//                    log.used        = timing.toMillSecond()
//                    log.summary     = context.logs.joinToString(NEW_LINE)
//                    logAsync.save(log)
//
//                    funResult
//                }
//                else{
//                    logger.info("使用 DEV 模式运行 FaaS，直接返回上下文对象...")
//                    context
//                }
//            }
//        }
//    }
}