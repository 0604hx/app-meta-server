package org.appmeta.component

import org.appmeta.domain.Page
import org.nerve.boot.module.setting.Setting
import org.springframework.context.ApplicationEvent
import java.io.Serializable

class PageDeleteEvent(val id:Serializable):ApplicationEvent(id)
class PageNameUpdateEvent(val page:Page):ApplicationEvent(page)
class PageContentUpdateEvent(val page: Page):ApplicationEvent(page)

class SettingChangeEvent(val setting:Setting): ApplicationEvent(setting)