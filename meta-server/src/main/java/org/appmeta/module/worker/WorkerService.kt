package org.appmeta.module.worker

import com.alibaba.fastjson2.JSON
import org.nerve.boot.Const.EMPTY
import org.nerve.boot.Result
import org.nerve.boot.db.service.BaseService
import org.nerve.boot.util.RSAProvider
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.nio.charset.StandardCharsets


/*
 * @project app-meta-server
 * @file    org.appmeta.module.worker.WorkerService
 * CREATE   2023年10月16日 18:06 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@Service
class WorkerService: BaseService<RemoteWorkerMapper, RemoteWorker>() {

    private val PORT = 9900

    val restTemplate = RestTemplate().also {
        //配置编码，避免传送时乱码
        it.messageConverters[1] = StringHttpMessageConverter(StandardCharsets.UTF_8)
    }

    fun call(worker: RemoteWorker, task: WorkerTask) {
        val result = restTemplate.postForObject(
            "http://${worker.id}:${PORT}",
            HttpEntity(RSAProvider(worker.pubKey, null).encrypt(JSON.toJSONString(task))),
            Result::class.java
        ) ?: throw Exception("调用远程Worker失败：响应为空")

        logger.info("调用 Worker/${worker.id} {}", JSON.toJSONString(result))
        println(JSON.toJSONString(result))
    }
}