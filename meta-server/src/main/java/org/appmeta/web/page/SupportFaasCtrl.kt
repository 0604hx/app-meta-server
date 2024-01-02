package org.appmeta.web.page

import com.alibaba.fastjson2.JSON
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import org.apache.commons.lang3.exception.ExceptionUtils
import org.appmeta.ALL
import org.appmeta.F.ID
import org.appmeta.F.TEMPLATE
import org.appmeta.H
import org.appmeta.Role
import org.appmeta.domain.Page
import org.appmeta.domain.Page.Companion.FAAS
import org.appmeta.domain.TerminalLog
import org.appmeta.model.IdModel
import org.appmeta.module.faas.FaasService
import org.appmeta.module.faas.Func
import org.appmeta.module.faas.FuncContext
import org.appmeta.module.faas.UserContext
import org.appmeta.service.*
import org.appmeta.tool.AuthHelper
import org.appmeta.web.AnonymousAbleCtrl
import org.nerve.boot.Const.EMPTY
import org.nerve.boot.Const.NEW_LINE
import org.nerve.boot.domain.AuthUser
import org.nerve.boot.util.Timing
import org.springframework.http.HttpHeaders
import org.springframework.util.StreamUtils
import org.springframework.util.StringUtils.hasText
import org.springframework.web.bind.annotation.*

/*
 * @project app-meta-server
 * @file    org.appmeta.web.page.SupportFaasCtrl
 * CREATE   2023年12月27日 16:57 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@RestController
class SupportFaasCtrl(
    private val pageS: PageService,
    private val appRoleS: AppRoleService,
    private val authHelper: AuthHelper,
    private val accountHelper: AccountHelper,
    private val accountService: AccountService,
    private val service:FaasService,
    private val logAsync: LogAsync
) : AnonymousAbleCtrl() {

    private fun _error(msg:String, page:Page):Void = throw Exception("$msg，请联系管理者<${page.uid}>")

    private fun buildUserContext(user:AuthUser, aid:String) = UserContext(user).also {
        val cache = appRoleS.loadRoleAndAuthOfUser(aid, user.ip)
        it.appRoles = cache.first
        it.appAuths = cache.second
    }

    /**
     * 调用日志记录到 TerminalLog
     * host 为功能 ID
     * code 恒定为 -1
     * url  为参数
     * summary 为函数输出的日志+报错信息
     */
    @RequestMapping("faas/{id}")
    fun faas(@PathVariable id:Long) = resultWithData {
        val contentType = request.getHeader(HttpHeaders.CONTENT_TYPE)?: EMPTY

        if(logger.isDebugEnabled){
            logger.debug("请求调用 FaaS 函数#${id} CONTENT_TYPE=${contentType}")
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
            if(hasText(jsonBody))
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
            if(user == null) UserContext.empty() else buildUserContext(user, page.aid)
        ).let { context->
            context.appendLog("[参数] ${JSON.toJSONString(originParams)}")
            service.execute( JSON.parseObject(pageS.buildContent(page, false), Func::class.java), context ).also { funResult->
                if(logger.isDebugEnabled)   logger.debug("FaaS 函数执行完成: {}", funResult)

                log.channel     = getChannel()
                log.method      = request.method
                log.uid         = if(user == null) EMPTY else user.id
                log.used        = timing.toMillSecond()
                log.summary     = context.logs.joinToString(NEW_LINE)
                logAsync.save(log)
            }
        }
    }

    inner class FaasDevModel: IdModel() {
        var func:Func?  = null
        var params      = mutableMapOf<String, Any>()
        var uid         = ""
    }

    @PostMapping("page/faas/dev")
    fun faasWithDev(@RequestBody model:FaasDevModel) = resultWithData {
        val page = pageS.getOne(QueryWrapper<Page>().eq(ID, model.id).eq(TEMPLATE, FAAS))?: throw Exception("FaaS函数不存在（请先创建再调试）")
        val user = authHolder.get()
        //判断权限
        if(!H.hasAnyRole(user, Role.DEVELOPER, Role.ADMIN))
            throw Exception("该功能仅限 ${Role.ADMIN}、${Role.DEVELOPER} 角色")
        if(!authHelper.checkEdit(page, user)) throw Exception("未授权编辑功能#${page.id}")

        if(model.func == null)  throw Exception("FaaS对象不能为空")

        val timing = Timing()
        model.func!!.let { func ->
            val context = FuncContext(
                page.aid,
                model.params,
                buildUserContext(if(hasText(model.uid)) accountService.toAuthUser(model.uid) else user , page.aid),
                true
            )
            context.appendLog("开始进行函数#${model.id}的模拟运行：\n\t[参数] ${model.params}\n\t[用户] ${context.user.id}")

            try {
                service.execute(func, context).also { funResult->
                    context.result = funResult
                }
            }catch (e:Exception){
                logger.error("模拟运行FaaS#${model.id}出错：${ExceptionUtils.getMessage(e)}")
                context.appendException(e)
            }finally {
                context.appendLog("\n函数执行完毕，耗时 ${timing.toSecondStr()} 秒")
                context
            }
        }
    }
}