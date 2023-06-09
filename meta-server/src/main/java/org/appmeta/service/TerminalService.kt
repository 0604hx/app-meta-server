package org.appmeta.service

import com.alibaba.fastjson2.JSON
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO
import org.appmeta.Caches
import org.appmeta.F
import org.appmeta.SERVER
import org.appmeta.domain.*
import org.appmeta.model.TerminalLogOverview
import org.nerve.boot.Const.EMPTY
import org.nerve.boot.Pagination
import org.nerve.boot.db.service.QueryHelper
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import java.util.*


/*
 * @project app-meta-server
 * @file    org.appmeta.service.TerminalService
 * CREATE   2023年04月06日 10:41 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 *
 * 应用后端服务
 */

@Service
class TerminalService(private val logM:TerminalLogMapper, private val pageM:PageMapper) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val queryHelper = QueryHelper<TerminalLog>()

    /**
     * 判断应用是否能使用对应的端口，不能重复
     */
    fun checkPortUsable(aid:String, port:Int): Boolean {
        val terminals = pageM.selectObjs(
                QueryWrapper<Page>().eq(F.TEMPLATE, SERVER).ne(F.AID, aid).select(F.CONTENT)
            )
            .map { JSON.parseObject(it.toString(), Terminal::class.java) }

        return terminals.firstOrNull { it.mode == Terminal.INSIDE && it.port == port } == null
    }

    @Cacheable(Caches.PAGE_SERVER)
    fun load(aid: String): Terminal? {
        val page = pageM.selectOne(
            QueryWrapper<Page>()
                .eq(F.TEMPLATE, SERVER)
                .eq(F.AID, aid).select(F.CONTENT, F.ID)
        )

        return JSON.parseObject(page.content, Terminal::class.java)?.also {  it.pid = "${page.id}" }
    }

    @Async
    fun addLog(log: TerminalLog) {
        if(log.addOn > 0L)
            log.used = System.currentTimeMillis() - log.addOn

        logM.insert(log)
    }

    fun logList(params:Map<String, Any>, pagination: Pagination, aid:String=EMPTY): List<TerminalLog> {
        val p = PageDTO<TerminalLog>(
            pagination.page.toLong(),
            pagination.pageSize.toLong()
        )
        val q = queryHelper.buildFromMap(params)
        if(StringUtils.hasText(aid))    q.eq(F.AID, aid)
        q.orderByDesc(F.ID)

        logM.selectPage(p, q)
        pagination.total = p.total
        return p.records
    }

    /**
     * 统计当日的数据
     */
    @Cacheable(Caches.PAGE_TERMINAL)
    fun logOverview(aid: String): TerminalLogOverview {
        var total   = 0L
        var today   = 0L
        var used    = 0L
        var error   = 0L

        val time    = with(Calendar.getInstance()) {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)

            timeInMillis
        }
        logM.streamByAid(aid) { c->
            val log = c.resultObject
            total   ++
            used += log.used
            if(log.code != 200) error ++
            if(log.addOn >= time) today ++
        }

        return TerminalLogOverview(aid, total, today, used, error)
    }
}