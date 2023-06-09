<div align=center>
<h1>🎉 应用元宇宙 / APP META 🎉</h1>

![Language](https://img.shields.io/github/languages/top/0604hx/app-meta-server?logo=javascript&color=blue)
![License](https://img.shields.io/badge/License-MIT-green)
![LastCommit](https://img.shields.io/github/last-commit/0604hx/app-meta-server?color=blue&logo=github)

</div>

```text
 _______  _______  _______    __   __  _______  _______  _______    _______  _______  ______    __   __  _______  ______
|   _   ||       ||       |  |  |_|  ||       ||       ||   _   |  |       ||       ||    _ |  |  | |  ||       ||    _ |
|  |_|  ||    _  ||    _  |  |       ||    ___||_     _||  |_|  |  |  _____||    ___||   | ||  |  |_|  ||    ___||   | ||
|       ||   |_| ||   |_| |  |       ||   |___   |   |  |       |  | |_____ |   |___ |   |_||_ |       ||   |___ |   |_||_
|       ||    ___||    ___|  |       ||    ___|  |   |  |       |  |_____  ||    ___||    __  ||       ||    ___||    __  |
|   _   ||   |    |   |      | ||_|| ||   |___   |   |  |   _   |   _____| ||   |___ |   |  | | |     | |   |___ |   |  | |
|__| |__||___|    |___|      |_|   |_||_______|  |___|  |__| |__|  |_______||_______||___|  |_|  |___|  |_______||___|  |_|
```

> 基于 [SpringBoot3](https://spring.io/projects/spring-boot) + [VUE3](https://cn.vuejs.org/) + [Naive UI](https://www.naiveui.com) + [Electron](https://www.electronjs.org) 应用快速开发、发布平台，旨在帮助使用者（包含但不限于开发人员、业务人员）快速响应业务需求，此仓库为后端，前端仓库详见[app-meta](https://github.com/0604hx/app-meta)。

## 附录

### 二次开发
> Spring Boot 3 至少需要 JDK 17，若您的 JDK 不在此范围，请先升级

#### 如何打包

在根目录执行 `mvn package -pl meta-server -am -amd`（windows 平台可直接使用 `pacage.bat`）

### 工具库

名称| 简介 |备注
---|---|---
[handlebars.java](https://github.com/jknack/handlebars.java)|Logic-less and semantic Mustache templates with Java（模版引擎）

### 备用库

名称| 简介 |备注
---|---|---
[sofa-ark](https://github.com/sofastack/sofa-ark)| SOFAArk 是一款基于 Java 实现的动态热部署和轻量级类隔离框架，由蚂蚁集团开源贡献，主要提供应用模块的动态热部署和类隔离能力。基于 Fat Jar 技术，可以将多个应用模块打包成一个自包含可运行的 Fat Jar，应用既可以是简单的单模块 Java 应用也可以是 SpringBoot/SOFABoot 应用 |用于动态部署 Java 类后端服务？
[NuProcess](https://github.com/brettwooldridge/NuProcess)| Low-overhead, non-blocking I/O, external Process implementation for Java |执行系统命令
[zt-exec](https://github.com/zeroturnaround/zt-exec)|ZeroTurnaround Process Executor|执行系统命令
