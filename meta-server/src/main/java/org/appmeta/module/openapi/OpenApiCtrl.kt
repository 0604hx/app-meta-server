package org.appmeta.module.openapi

import com.alibaba.fastjson2.JSON
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import org.appmeta.F
import org.appmeta.model.IdModel
import org.appmeta.model.QueryModel
import org.appmeta.service.CacheRefresh
import org.nerve.boot.module.operation.Operation
import org.nerve.boot.util.Timing
import org.nerve.boot.web.ctrl.BasicController
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.*


/*
 * @project app-meta-server
 * @file    org.appmeta.module.openapi.OpenApiCtrl
 * CREATE   2023年06月07日 17:30 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@RestController
@RequestMapping("api")
class OpenApiCtrl(private val service: ApiService, private val detailM:ApiDetailMapper) : BasicController() {

    @PostMapping("query", name = "开放接口清单")
    fun list(@RequestBody model:QueryModel) = resultWithData {
        service.query(model)
    }

    @PostMapping("{id}", name = "调用开放接口")
    fun call(@PathVariable id:Long, @RequestBody ps:Map<String, Any>) = resultWithData {
        val user = authHolder.get()
        val timing = Timing()
        val api = service.getById(id)?: throw Exception("开放接口 #$id 不存在")

        val items = service.call(user, api, ps)

        opLog(
            "${user.showName} 调用$api${if(ps.isEmpty()) "" else ",参数=${JSON.toJSONString(ps)}"}，耗时${timing.toMillSecond()}ms，数据量 ${items.size}",
            api
        )

        items
    }
}

@RestController
@RequestMapping("system/api")
class OpenApiManageCtrl(
    private val cacheRefresh: CacheRefresh,
    private val mapper:ApiMapper,
    private val service: ApiService, private val detailM:ApiDetailMapper):BasicController(){

    @PostMapping("edit", name = "编辑开放接口")
    fun edit(@RequestBody api: Api) = resultWithData {
        val user = authHolder.get()
        if(!StringUtils.hasText(api.uid))    api.uid = user.id
        service.createOrUpdate(api)
        api.id
    }

    @PostMapping("detail", name = "获取接口详细")
    fun detail(@RequestBody model:IdModel) = resultWithData { detailM.withCache(model.id) }

    @PostMapping("delete", name = "删除开放接口")
    fun delete(@RequestBody model:IdModel) = result {
        mapper.selectById(model.id)?.let {
            mapper.selectCount(QueryWrapper<Api>().eq(F.PID, it.id)).also { size->
                if(size > 0)
                    throw Exception("$it 存在 $size 个子接口，不能删除")
            }


            mapper.deleteById(model.id)
            detailM.deleteById(model.id)

            cacheRefresh.api(model.id)
            opLog("删除开放接口$it", it, Operation.DELETE)
        }
    }

    @PostMapping("update-detail", name = "更新接口详细")
    fun updateDetail(@RequestBody detail: ApiDetail) = result {
        val hasOld = detailM.exists(QueryWrapper<ApiDetail>().eq(F.ID, detail.id))
        if(hasOld)
            detailM.updateById(detail)
        else
            detailM.insert(detail)

        cacheRefresh.api(detail.id)
        opLog("${if(hasOld) "更新" else "创建"}开放接口#${detail.id}的详细内容", detail)
    }
}