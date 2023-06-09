package org.appmeta.web

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Template
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.lang3.exception.ExceptionUtils
import org.appmeta.F
import org.appmeta.S
import org.appmeta.S.SYS_TERMINAL_HEADER_VALUE
import org.appmeta.component.AppConfig
import org.appmeta.component.ServiceRoute
import org.appmeta.component.SettingChangeEvent
import org.appmeta.domain.Terminal
import org.appmeta.domain.TerminalLog
import org.appmeta.service.TerminalService
import org.nerve.boot.Result
import org.nerve.boot.domain.AuthUser
import org.nerve.boot.module.setting.SettingService
import org.nerve.boot.web.ctrl.BasicController
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils.hasText
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URLEncoder


/*
 * @project app-meta-server
 * @file    org.appmeta.web.ProxyCtrl
 * CREATE   2023年03月22日 09:09 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 *
 * 请求转发，文章参考
 *
 * spring boot通用转发请求                     https://blog.csdn.net/liufang1991/article/details/129559677
 * spring boot实现超轻量级网关（反向代理及转发）   https://blog.csdn.net/m0_37657556/article/details/121159548
 */


@Service
class ProxyService(private val settingS:SettingService, private val config: AppConfig){
    val logger = LoggerFactory.getLogger(javaClass)

    lateinit var template:Template

    @PostConstruct
    private fun init(){
        var value = settingS.value(SYS_TERMINAL_HEADER_VALUE)
        if(!hasText(value))
            value = "{{ ${F.ID} }}-{{ ${F.NAME} }}-{{ ${F.IP} }}"
        template = Handlebars().compileInline(value)
    }

    @EventListener(SettingChangeEvent::class, condition = "#e.setting.id=='SYS_TERMINAL_HEADER_VALUE'")
    fun onSettingChange(e: SettingChangeEvent) {
        val setting = e.setting
        if(logger.isDebugEnabled)   logger.debug("检测到 $SYS_TERMINAL_HEADER_VALUE 变动：${setting.content}")

        template = Handlebars().compileInline(setting.content)
        logger.info("[TEMPLATE] $SYS_TERMINAL_HEADER_VALUE 模版更新为 ${template.text()}")
    }

    fun buildHeader(user:AuthUser) = mapOf(
        "from"                                      to config.name,
        settingS.value(S.SYS_TERMINAL_HEADER_NAME)  to template.apply(
            mapOf(
                F.ID    to user.id,
                F.NAME  to URLEncoder.encode(user.name, Charsets.UTF_8.name()),
                F.IP    to user.ip,
                F.TIME  to System.currentTimeMillis()
            )
        )
    )

}

@RestController
class ProxyCtrl(
    private val settingS: SettingService,
    private val service: ProxyService,
    private val config: AppConfig, private val route: ServiceRoute, private val terminalS:TerminalService) : BasicController(){

    @RequestMapping("service/{aid}/**", name = "应用后台服务")
    fun redirect(@PathVariable aid:String, response:HttpServletResponse):ResponseEntity<*> {
        val path = request.servletPath.replace("/service/${aid}", "")

        val terminal = terminalS.load(aid) ?: throw Exception("应用⌈$aid⌋未开通后端服务")
        var url = if(terminal.mode == Terminal.OUTSIDE) terminal.url else "${settingS.value(S.SYS_TERMINAL_HOST)}:${terminal.port}"

        url = "${url}${path}${if(hasText(request.queryString)) "?${request.queryString}" else ""}"

        if(logger.isDebugEnabled)    logger.debug("转发请求（APP=$aid） 到 ${url}${path}")

        val user = authHolder.get()

        val log = TerminalLog(aid, url)
        log.method  = request.method
        log.uid = user.id

        return try{
            route.redirect( request, response, url, service.buildHeader(user) ).also {
                log.code = it.statusCode.value()
            }
        }
        catch (e:Exception) {
            logger.error("[SERVICE-ROUTE] 转发失败", e)
            log.code = HttpStatus.INTERNAL_SERVER_ERROR.value()
            log.summary = ExceptionUtils.getMessage(e)

            ResponseEntity(Result.fail(e), HttpStatus.INTERNAL_SERVER_ERROR)
        }
        finally {
            terminalS.addLog(log)
            if(logger.isDebugEnabled)   logger.debug("[SERVICE-ROUTE] 转发请求到 ${log.url} (${log.used} ms)")
        }
    }
}