package org.appmeta.web.app

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import jakarta.validation.Valid
import org.appmeta.F
import org.appmeta.H
import org.appmeta.Role
import org.appmeta.component.AppConfig
import org.appmeta.domain.*
import org.appmeta.model.*
import org.appmeta.service.AppAsync
import org.appmeta.service.AppService
import org.appmeta.service.CacheRefresh
import org.appmeta.tool.LimitMap
import org.nerve.boot.Const.EMPTY
import org.nerve.boot.Result
import org.nerve.boot.domain.AuthUser
import org.nerve.boot.exception.ServiceException
import org.nerve.boot.module.operation.Operation
import org.nerve.boot.web.ctrl.BasicController
import org.springframework.http.HttpStatus
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.Serializable


/*
 * @project app-meta-server
 * @file    org.appmeta.web.app.AppCtrl
 * CREATE   2022年12月06日 13:39 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@RestController
@RequestMapping("app")
class AppCtrl(
    private val refresh: CacheRefresh,
    private val appAsync: AppAsync,
    private val pageM:PageMapper,
    private val dataM:DataMapper,
    private val mapper: AppMapper, private val service: AppService) : BasicController() {

    protected fun _checkEditAuth(id: Serializable, worker:(App, AuthUser)->Any?): Result {
        val app = mapper.selectById(id)
        val user = authHolder.get()
        if(app.uid == user.id || H.hasAnyRole(user, Role.ADMIN, Role.APP_MANAGER))
            return resultWithData { worker(app, user) }

        throw Exception(HttpStatus.UNAUTHORIZED.name)
    }

    @RequestMapping("top", name = "最新TOP10")
    fun top(@RequestBody model: KeyModel) = resultWithData {
        val list = mapper.loadOrderBy(
            when (model.key) {
                F.LAUNCH    -> F.LAUNCH
                F.MARK      -> F.MARK
                else        -> F.ADD_ON
            }
        )
        list
    }

    @RequestMapping("launch", name = "运行应用")
    fun launch(@RequestBody model: PageModel) = result {
        val user = authHolder.get()
        appAsync.afterLaunch(
            model,
            if(user == null) EMPTY else user.id,
            requestIP
        )
    }

    @RequestMapping("list", name = "应用列表查询")
    fun list(@RequestBody model: QueryModel) = result {
        it.data = service.list(model.form, model.pagination)
        it.total= model.pagination.total
    }

    @RequestMapping("list-mine", name = "我授权的应用列表")
    fun listOfMine(@RequestBody model: QueryModel) = result {
        val user = authHolder.get()
        model.form["EQ_uid"] = user.id

        it.data = service.list(model.form, model.pagination)
        it.total= model.pagination.total
    }

    @RequestMapping("detail", name = "获取应用基本信息")
    fun detail(@RequestBody model: IdStringModel) = resultWithData {
        service.detailOf(model.id)
    }

    @RequestMapping("create", name = "创建新应用")
    fun create(@Valid @RequestBody model: AppModel) = resultWithData {
        val user = authHolder.get()
        val app = model.app

        app.of(user)

        service.create(model)
        opLog("新增应用⌈${app.name}⌋ 作者=${app.author}", app, Operation.CREATE)

        "⌈${app.name}⌋创建成功"
    }

    @RequestMapping("update", name = "更新应用信息")
    fun update(@Valid @RequestBody model: AppModel) = _checkEditAuth(model.app.id) { _, user->
        val app = model.app
        if(StringUtils.hasText(app.uid))
            app.of(user)

        service.update(model)
        opLog("更新应用⌈${app.name}⌋ 作者=${app.author}", app, Operation.MODIFY)

        "应用⌈${app.name}⌋更新成功"
    }

    @PostMapping("modify", name = "修改应用属性")
    fun modifyField(@RequestBody model: FieldModel) = _checkEditAuth(model.id) { app, _->
        val wrapper = UpdateWrapper<App>()
        when(model.key) {
            F.ACTIVE    -> {
                mapper.update(null, wrapper.eq(F.ID, model.id).set(F.ACTIVE, model.value))
            }
            else        -> throw Exception("暂不支持修改属性 ${model.key}")
        }

        opLog("修改应用#${model.id} 的属性 ${model.key}=${model.value}", app)
    }

    @RequestMapping("delete", name = "删除应用")
    fun delete(@RequestBody model: IdStringModel) = resultWithData {
        val (id) = model
        val user = authHolder.get()

        val app = service.getById(id)
        //仅限应用创建者及管理员
        if (app.uid == user.id || user.hasRole(Role.ADMIN)) {
            val dataCount = dataM.selectCount(QueryWrapper<Data>().eq(F.AID, id))
            if(dataCount > 0L){
                throw ServiceException("应用⌈${id}/${app.name}⌋下有 $dataCount 条数据，请先处理再操作")
            }
            val pageCount = pageM.selectCount(QueryWrapper<Page>().eq(F.AID, id))
            if(pageCount > 0L)
                throw ServiceException("应用⌈${id}/${app.name}⌋下有 $pageCount 个页面，请先处理再操作")

            service.removeById(id)
            refresh.app(id)

            opLog("删除应用#${id}", app, Operation.DELETE)
        }
        else
            throw Exception("仅限创建者或管理员能够删除应用")
    }
}