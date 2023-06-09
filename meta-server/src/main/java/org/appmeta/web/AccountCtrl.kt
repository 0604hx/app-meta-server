package org.appmeta.web

import org.appmeta.service.AccountService
import org.nerve.boot.web.ctrl.BasicController
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


/*
 * @project app-meta-server
 * @file    org.appmeta.web.AccountCtrl
 * CREATE   2023年02月23日 19:36 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@RestController
@RequestMapping("account")
class AccountCtrl(private val service:AccountService) : BasicController() {

    @PostMapping("list", name = "用户及部门清单")
    fun list() = resultWithData {
        service.listOfAll()
    }

    @PostMapping("departs", name = "部门清单")
    fun departList() = resultWithData { service.listOfDepart() }

    @PostMapping("roles", name = "角色清单")
    fun roleLise() = resultWithData { service.listOfRole() }
}