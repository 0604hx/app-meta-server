package org.appmeta.module.openapi

import com.alibaba.fastjson2.JSON
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.toolkit.SqlRunner
import com.github.jknack.handlebars.Handlebars
import io.jsonwebtoken.lang.Assert
import org.appmeta.Caches
import org.appmeta.F
import org.appmeta.model.QueryModel
import org.appmeta.tool.AuthHelper
import org.nerve.boot.db.service.BaseService
import org.nerve.boot.domain.AuthUser
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import java.io.Serializable


/*
 * @project app-meta-server
 * @file    org.appmeta.module.openapi.Service
 * CREATE   2023年06月07日 15:55 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@Service
class ApiService(
    private val authHelper: AuthHelper,
    private val detailM:ApiDetailMapper) : BaseService<ApiMapper, Api>() {

    override fun getById(id: Serializable) = baseMapper.withCache(id as Long)

    private fun DQ() = QueryWrapper<ApiDetail>()

    fun query(model: QueryModel) = list(queryHelper.buildFromMap(model.form))

    @CacheEvict(Caches.API, key = "#api.id")
    fun createOrUpdate(api: Api) {
        Assert.hasText(api.name, "名称不能为空")

        api.pid?.also {
            //判断父接口是否存在
            Assert.isTrue(count(Q().eq(F.ID, it)) > 0, "父接口 #${it} 不存在")
        }

        val old = baseMapper.selectById(api.id)
        if(old == null){
            api.launch  = 0
            save(api)
        }
        else{
            api.launch = old.launch
            updateById(api)
        }

        if(!detailM.exists(DQ().eq(F.ID, api.id))){
            logger.info("检测到 ID=${api.id} 的接口详细不存在，现在创建...")
            detailM.insert(ApiDetail(api))
        }
    }

    private fun error(api: Api, msg:String):Nothing = throw Exception("${api}$msg")

    fun call(user: AuthUser, id:Long, ps:Map<String, Any>): List<Any> {
        val api = baseMapper.withCache(id) ?: throw Exception("开放接口 #$id 不存在")
        return call(user, api, ps)
    }

    /**
     * 1. 调用方发起请求，传递参数 `ps`
     * 2. 后端识别对应的接口，并判断接口是否生效（false 则中断）
     * 3. 判断当前用户是否授权使用（false 则中断）
     * 4. 解析参数，判断参数是否合规（false 则中断）
     * 5. 若设置了`sourceId`，需要进一步判断数据源的可用性（false 则中断）
     * 6. 拼凑 SQL 并执行
     * 7. 按照 `resultType` 封装结果并返回
     * 8. `launch` +1 并记录日志
     */
    fun call(user:AuthUser, api: Api, ps:Map<String, Any>):List<Any> {
        val detail = detailM.withCache(api.id) ?:   error(api, "尚未完善")

        if(!detail.active)                          error(api, "尚未开放")
        if(!authHelper.checkService(detail, user))  error(api, "未授权")
        if(!StringUtils.hasText(detail.cmd))        error(api, "指令未填写")

        if(StringUtils.hasText(detail.params)){
            val parmeters = JSON.parseArray(detail.params, ApiParmeter::class.java)
            parmeters.onEach {
                // 赋予默认值
                if(StringUtils.hasText(it.value) && !ps.containsKey(it.id))
                    ps[it.id]
                if(it.required && (!ps.containsKey(it.id) || !StringUtils.hasText(ps[it.id].toString())))
                    error(api, "参数 ${it.id}/${it.name} 为必填")
                //检验
                if(StringUtils.hasText(it.regex)){
                    if(!Regex(it.regex).matches("${ps[it.id]}"))    error(api, "参数 ${it.id}/${it.name} 格式不合规")
                }
            }
        }

        val sql = Handlebars().compileInline(detail.cmd).apply(ps)
        logger.info("[开发接口] #${api.id} 执行 $sql")

        return if(detail.sourceId == null){
            val items = SqlRunner.db().selectList(sql)
            if(detail.resultType == ApiDetail.ARRAY)
                items.map { it.values }
            else {
                items
            }
        }
        else{
            error(api, "暂不支持其他数据源")
        }.also {
            api.launch += 1
            updateById(api)
        }
    }
}