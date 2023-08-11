package org.appmeta.component

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.util.MultiValueMap
import org.springframework.util.StreamUtils
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.*


@Component
class ServiceRoute {
    val logger = LoggerFactory.getLogger(javaClass)

    val restTemplate = RestTemplate().also {  }

    fun redirect(request:HttpServletRequest, response:HttpServletResponse, targetUrl:String, headers: Map<String, String?>?=null):ResponseEntity<ByteArray> {
        val entity = createRequestEntity(request, targetUrl, headers)
        return restTemplate.exchange(entity, ByteArray::class.java)
    }

    @Throws(URISyntaxException::class, IOException::class)
    private fun createRequestEntity(request: HttpServletRequest, url: String, extraHeaders: Map<String, String?>?): RequestEntity<*> {
        val httpMethod = HttpMethod.valueOf(request.method)
        val headers = parseRequestHeader(request)
        extraHeaders?.forEach { (k, v) -> headers.add(k, v) }

        //将原始请求转换为字节数组
        val body = StreamUtils.copyToByteArray(request.inputStream)
        return RequestEntity<Any>(body, headers, httpMethod, URI(url))
    }

    /**
     * 复制原始请求的 header 信息
     */
    private fun parseRequestHeader(request: HttpServletRequest): MultiValueMap<String, String?> {
        val headers = HttpHeaders()
        val headerNames: List<String> = Collections.list(request.headerNames)
        for (headerName in headerNames) {
            val headerValues: List<String> = Collections.list(request.getHeaders(headerName))
            for (headerValue in headerValues) {
                headers.add(headerName, headerValue)
            }
        }
        return headers
    }
}