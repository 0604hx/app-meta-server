package org.appmeta.service

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import org.appmeta.F
import org.appmeta.S
import org.appmeta.component.AppConfig
import org.appmeta.domain.*
import org.appmeta.model.PageModel
import org.appmeta.tool.LimitMap
import org.nerve.boot.Const.EMPTY
import org.nerve.boot.module.setting.Setting
import org.nerve.boot.module.setting.SettingService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import kotlin.math.abs


/*
 * @project app-meta-server
 * @file    org.appmeta.service.AsyncService
 * CREATE   2023年01月10日 13:33 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@Service
class AppAsync(
    private val config: AppConfig,
    private val mapper: AppMapper,
    private val launchM:PageLaunchMapper,
    private val pageM:PageMapper) {

    val logger = LoggerFactory.getLogger(javaClass)

    private val launchMap = LimitMap<Long>(500)

    /**
     * 数值类型字段的增减
     */
    private fun modifyIncrement(id:String, field:String, step:Int = 1){
        mapper.update(
            null,
            UpdateWrapper<App>()
                .eq(F.ID, id)
                .setSql("$field = $field ${if(step<0) "-" else "+"} ${abs(step)}")
        )
        logger.info("[APP-ASYNC] #$id $field += $step")
    }

    @Async
    fun afterLaunch(model: PageModel, uid:String, ip:String=EMPTY) {
        val key = "${model}-${uid}-${model.channel}"
        val cur = System.currentTimeMillis()

        if(cur - launchMap.getOrDefault(key, 0L) > config.appLaunchWindow*60*1000){
            modifyIncrement(model.aid, F.LAUNCH)
            //更新页面运行计数
            pageM.onLaunch(model.pid)

            if(StringUtils.hasText(model.pid)){
                //记录信息
                PageLaunch(model).also {
                    it.uid      = uid
                    it.ip       = ip
                    it.channel  = model.channel

                    launchM.insert(it)
                }
            }
        }
        launchMap[key] = cur
    }

    @Async
    fun afterMark(id: String, isRemove:Boolean = false) = modifyIncrement(id, F.MARK, if(isRemove) -1 else 1)

    @Async
    fun afterLike(id: String) = modifyIncrement(id, F.THUMB)
}

/**
 * 系统级别的异步处理
 */
@Service
class SystemAsync(private val accountS:AccountService,private val settingS:SettingService) {
    val logger = LoggerFactory.getLogger(javaClass)

    private fun _log(msg:String) = logger.info("[系统异步作业] $msg")

    @Async
    fun onSettingChange(setting: Setting) {
        when (setting.id) {
            S.SYS_ACCOUNT_REMOTE.name -> {
                if(StringUtils.hasText(setting.content) && settingS.intValue(S.SYS_ACCOUNT_INTERVAL, 0) > 0){
                    _log("检测到用户同步地址变更，即将进行数据同步...")
                    // 立即进行用户数据同步
                    accountS.refreshFromRemote(setting.content)
                }
            }
        }
    }
}