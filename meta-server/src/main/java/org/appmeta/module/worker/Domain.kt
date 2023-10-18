package org.appmeta.module.worker

import com.baomidou.mybatisplus.annotation.TableName
import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.appmeta.domain.SummaryBean
import org.nerve.boot.annotation.CN


/*
 * @project app-meta-server
 * @file    org.appmeta.module.worker.Domain
 * CREATE   2023年10月16日 17:49 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

/**
 * 任务对象
 */
class WorkerTask {
    var uid         = ""    //绑定的用户ID
    var method      = ""
    var params      = mutableMapOf<String, Any>()
    var time        = 0L

    constructor()
    constructor(method:String, params:Map<String, Any>){
        this.method = method
        this.params = params.toMutableMap()
        this.time   = System.currentTimeMillis()
    }
}

@CN("远程工作者")
@TableName("worker")
class RemoteWorker: SummaryBean {
    var priKey      = ""        //私钥
    var pubKey      = ""        //公钥
    var lastTime    = 0L        //最后使用时间
    var addOn       = 0L

    constructor()
    constructor(ip:String){
        setId(ip)
    }
}

interface RemoteWorkerMapper:BaseMapper<RemoteWorker>