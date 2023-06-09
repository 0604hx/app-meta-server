package org.appmeta.domain

import com.baomidou.mybatisplus.annotation.TableName
import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.Mapper
import org.nerve.boot.annotation.CN


/*
 * @project app-meta-server
 * @file    org.appmeta.domain.Outreach
 * CREATE   2023年04月14日 09:23 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 *
 * 提供快捷登录的终端
 */

@CN("会员终端")
@TableName("member")
class Member : SummaryBean {
    var ids     = ""        //允许登录的ID，多个用英文逗号隔开
    var secret  = ""        //AES 密钥
    var expire  = 60        //令牌有限期，默认是 60 分钟
    var addOn   = 0L

    constructor()
    constructor(ip:String) {
        id  = ip
    }
}

@Mapper
interface MemberMapper:BaseMapper<Member>