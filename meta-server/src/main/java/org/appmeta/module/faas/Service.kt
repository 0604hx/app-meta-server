package org.appmeta.module.faas

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils


/*
 * @project app-meta-server
 * @file    org.appmeta.module.faas.Service
 * CREATE   2023年12月26日 15:45 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@Service
class FaasService(private val sqlExecutor: SQLExecutor, private val jsExecutor: JSExecutor) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val TRUE_VALUES = listOf("true", "1")

    /**
     * 参数校验与修复
     */
    private fun checkParams(func: Func, params: MutableMap<String, Any>){
        func.params.onEach { p ->
            // 赋予默认值
            if(StringUtils.hasText(p.value) && !params.containsKey(p.id))
                params[p.id] = p.value!!

            if(p.required && (!params.containsKey(p.id) || !StringUtils.hasText(params[p.id].toString())))
                throw Exception("参数 ${p.id}/${p.name} 为必填")

            //检验
            if(StringUtils.hasText(p.regex)){
                if(!Regex(p.regex).matches("${params[p.id]}"))    throw Exception("参数 ${p.id}/${p.name} 格式不合规")
            }

            //类型转换
            if(params[p.id] is String){
                val v = params[p.id] as String

                params[p.id] = when(p.type){
                    FuncParmeter.NUMBER     -> v.toLong()
                    FuncParmeter.BOOLEAN    -> TRUE_VALUES.contains(v.lowercase())
                    else                    -> v
                }
            }
        }

        if(func.paramsLimit){
            val ids = func.params.map { it.id }
            params.keys.filter { !ids.contains(it) }.forEach{ k-> params.remove(k) }
        }
    }

    /**
     * 执行用户自定义函数
     */
    fun execute(func:Func, context: FuncContext):Any? {
        checkParams(func, context.params)
        if(logger.isDebugEnabled)   logger.debug("修正后参数：${context.params}")

        return when(func.mode){
            Func.SQL    -> sqlExecutor.run(func, context)
            Func.JS     -> jsExecutor.run(func, context)
            else        -> throw Exception("无效的类型<${func.mode}>")
        }
    }
}