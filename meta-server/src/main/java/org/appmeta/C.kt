package org.appmeta

import org.nerve.boot.domain.AuthUser
import org.nerve.boot.util.DateUtil

const val URL_ALL   = "/**"
const val ALL       = "**"
const val ANY       = "*"
const val SERVER    = "server"
val IS_WINDOW       = System.getProperty("os.name").uppercase().contains("WINDOW")

object Channels {
    const val BROWSER   = "browser"     //常规浏览器
    const val CLIENT    = "client"      //客户端
    const val MOBILE    = "mobile"      //移动端
    const val CLI       = "cli"         //终端命令行
}

object F {
    const val ID        = "id"
    const val AID       = "aid"
    const val PID       = "pid"
    const val UID       = "uid"
    const val OID       = "oid"
    const val UNAME     = "uname"
    const val NAME      = "name"
    const val SUMMARY   = "summary"
    const val ROLE      = "role"
    const val TEXT      = "text"
    const val CONTENT   = "content"
    const val VALUE     = "value"
    const val UUID      = "uuid"
    const val FROM_DATE = "fromDate"
    const val TO_DATE   = "toDate"

    const val TYPE      = "type"
    const val LAUNCH    = "launch"
    const val MARK      = "mark"
    const val THUMB     = "thumb"

    const val ADD_ON    = "addOn"
    const val DONE_ON   = "doneOn"

    const val SEARCH    = "search"
    const val ACTIVE    = "active"
    const val AUTH      = "auth"
    const val MAIN      = "main"
    const val TEMPLATE  = "template"
    const val LABEL     = "label"

    const val SERVICE_AUTH  = "serviceAuth"
    const val EDIT_AUTH     = "editAuth"

    const val IP        = "ip"
    const val TIME      = "time"

    const val SOURCE_ID = "sourceId"

    const val BATCH     = "batch"
    const val BATCH_    = "_batch_"
}

enum class Forms {
    TEXT,
    NUMBER,
    SWITCH,
    RADIO,
    SELECT,
    DATE,
    HIDE,
    INFO,
    FILE
}

enum class Role {
    NORMAL,
    ADMIN,
    DEPLOYER,

    DBM,
    DBM_ADMIN,
    APP_MANAGER,

    SYS_ADMIN
}

enum class S {
    COMMON,
    COM_NAME,

    PICTURE_SUFFIX,
    PICTURE_WIDTH,

    AUTH_AES_KEY,
    AUTH_METHOD,
    AUTH_JWT_EXPIRE,
    AUTH_JWT_KEY,
    AUTH_CAS_HOST,
    AUTH_CAS_PARAM,
    AUTH_CAS_SECRET,
    AUTH_CAS_URL,

    APP_MICRO_INJECT,

    DBM_LIMIT,
    DBM_BATCH,

    SYS_NOTICE_LINE,
    SYS_ACCOUNT_INTERVAL,
    SYS_ACCOUNT_REMOTE,

    SYS_TERMINAL_HOST,
    SYS_TERMINAL_HEADER_NAME,
    SYS_TERMINAL_HEADER_VALUE,

    SYS_ROBOT_TRACE,

    SYS_WHITE_IP,
}

class Auth {
    object Methods {
        const val PWD = "PWD"
        const val CAS = "CAS"
        const val PICK= "PICK"      //从登陆后的页面中提取用户ID
    }
}

object Caches {
    const val ACCOUNT       = "ACCOUNT"

    const val AUTH_USER     = "AUTH_USER"

    const val APP           = "APP"
    const val APP_ACCOUNT   = "APP_ACCOUNT"

    const val PAGE_LIST     = "PAGE_LIST"
    const val PAGE_LINK     = "PAGE_LINK"
    const val PAGE_LINK_LIST= "PAGE_LINK_LIST"
    const val PAGE_SERVER   = "PAGE_SERVER"
    const val PAGE_TERMINAL = "PAGE_TERMINAL"
    const val PAGE_DOCUMENT = "PAGE_DOCUMENT"

    const val NOTICE_LIST   = "NOTICE_LIST"

    const val SYS_OVERVIEW  = "SYS_OVERVIEW"

    const val DBM_SOURCE    = "DBM_SOURCE"

    const val API           = "API"
    const val API_DETAIL    = "API_DETAIL"
}

object H {
    fun buildVersion(): String = DateUtil.getDate("yy.M.d")

    /**
     * 判断是否具备某个给定权限
     */
    fun hasAnyRole(user:AuthUser, vararg roles:Role) = roles.any { user.hasRole(it) }

    /**
     * 判断是否具备全部给定权限
     */
    fun hasAllRole(user: AuthUser, vararg roles: Role) = roles.all { user.hasRole(it) }
}