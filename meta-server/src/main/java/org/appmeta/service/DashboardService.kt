package org.appmeta.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import org.appmeta.Caches
import org.appmeta.F
import org.appmeta.F.LABEL
import org.appmeta.F.TEMPLATE
import org.appmeta.F.VALUE
import org.appmeta.domain.*
import org.appmeta.model.OverviewResultModel
import org.nerve.boot.util.DateUtil
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.lang.management.ManagementFactory
import java.util.*


/*
 * @project app-meta-server
 * @file    org.appmeta.service.DashboardService
 * CREATE   2023年03月30日 11:54 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@Service
class DashboardService(
    private val pageM:PageMapper,
    private val pageLinkM:PageLinkMapper,
    private val documentM:DocumentMapper,
    private val dataM:DataMapper,
    private val appM: AppMapper) {

    private fun buildAmountItem(label:String, value:Number, suffix:String="个") = mapOf(
        LABEL     to label,
        VALUE     to value,
        "suffix"    to suffix
    )

    @Cacheable(Caches.SYS_OVERVIEW)
    fun overview():OverviewResultModel {
        val memHeap     = ManagementFactory.getMemoryMXBean().heapMemoryUsage
        val memNonHeap  = ManagementFactory.getMemoryMXBean().nonHeapMemoryUsage
        val sys         = ManagementFactory.getOperatingSystemMXBean()
        return OverviewResultModel(
            listOf(
                buildAmountItem("应用数", appM.selectCount(null)),
                buildAmountItem("页面 / 功能", pageM.selectCount(null)),
                buildAmountItem("关注", pageLinkM.selectCount(QueryWrapper<PageLink>().eq(F.ACTIVE, true)), "则"),
                buildAmountItem("数据量", dataM.selectCount(null), "条"),
                buildAmountItem("文档 / 附件", documentM.selectCount(null), "份")
            ),
            pageM
                .selectMaps(QueryWrapper<Page>().select("$TEMPLATE as $LABEL", "count(*) as $VALUE").groupBy(TEMPLATE))
                .associate { Pair(it[LABEL]!!, it[VALUE]) },
            pageM.selectList(QueryWrapper<Page>().select(F.NAME, F.LAUNCH).orderByDesc(F.LAUNCH).last("LIMIT 10"))
                .associate { Pair(it.name, it.launch) },
           mapOf(
               "started"    to DateUtil.formatDate(Date(ManagementFactory.getRuntimeMXBean().startTime)),
               "memory"     to (memHeap.used + memNonHeap.used) / 1024 / 1024,
               "cpu"        to "${sys.arch}/${sys.availableProcessors}核",
               "memoryMax"  to (memHeap.max + memNonHeap.max) / 1024/ 1024,
               "threads"    to ManagementFactory.getThreadMXBean().threadCount
           )
        )
    }
}