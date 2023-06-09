package org.appmeta.component

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.*
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

    /**
     *
     */
    fun redirect(request:HttpServletRequest, response:HttpServletResponse, targetUrl:String, headers: Map<String, String?>?=null):ResponseEntity<String> {
        val entity = createRequestEntity(request, targetUrl, headers)
        val restTemplate = RestTemplate()
        return restTemplate.exchange(entity, String::class.java)
    }

    @Throws(URISyntaxException::class, IOException::class)
    private fun createRequestEntity(request: HttpServletRequest, url: String, extraHeaders: Map<String, String?>?): RequestEntity<*> {
        val httpMethod = HttpMethod.valueOf(request.method)
        val headers = parseRequestHeader(request)
        extraHeaders?.forEach { (k, v) -> headers.add(k, v) }

//        if(logger.isDebugEnabled)   headers.forEach { (k, v) -> logger.debug("[HEADER] $k = $v") }

        val body = StreamUtils.copyToByteArray(request.inputStream)
        return RequestEntity<Any>(body, headers, httpMethod, URI(url))
    }

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