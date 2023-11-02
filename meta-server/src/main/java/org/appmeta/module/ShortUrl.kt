package org.appmeta.module

import com.baomidou.mybatisplus.annotation.TableName
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.mapper.BaseMapper
import jakarta.servlet.http.HttpServletResponse
import org.apache.ibatis.annotations.Mapper
import org.appmeta.F
import org.appmeta.model.TextModel
import org.nerve.boot.annotation.CN
import org.nerve.boot.db.StringEntity
import org.nerve.boot.util.MD5Util
import org.nerve.boot.web.ctrl.BasicController
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.util.Assert
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController


/*
 * @project app-meta-server
 * @file    org.appmeta.module.ShortUrl
 * CREATE   2023年11月02日 13:40 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@CN("短链接")
@TableName
class ShortUrl:StringEntity {
    var url     = ""

    constructor()
    constructor(id:String, url:String){
        setId(id)
        this.url = url
    }
}

@Mapper
interface ShortUrlMapper:BaseMapper<ShortUrl>

@Order(Int.MAX_VALUE)
@RestController("s")
class ShortUrlCtrl(private val mapper: ShortUrlMapper):BasicController() {
    @Value("\${server.servlet.context-path}")
    private val contextPath = ""

    @PostMapping("create", name = "生成短链接")
    fun create(@RequestBody model:TextModel) = resultWithData {
        Assert.isTrue(model.text.startsWith("/"), "长链接必须以 / 开头")

        MD5Util.encode(model.text).substring(8, 16).also {
            if (mapper.exists(QueryWrapper<ShortUrl>().eq(F.ID, it)))   throw Exception("短链接已存在")

            val short = ShortUrl(it, model.text)
            mapper.insert(short)
            logger.info("创建短链接 $it >> ${model.text}")
        }
    }

    @GetMapping("{uuid}", name = "短链接自动跳转")
    fun shortUrl(@PathVariable uuid:String, response: HttpServletResponse) {
        val short = mapper.selectById(uuid)?: throw Exception("短链接不存在")

        logger.info("短链接跳转 $uuid >> ${short.url}")
        response.sendRedirect("${contextPath}${short.url}")
    }
}