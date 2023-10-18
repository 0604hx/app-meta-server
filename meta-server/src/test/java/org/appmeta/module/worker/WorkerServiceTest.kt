package org.appmeta.module.worker

import org.appmeta.F
import org.junit.jupiter.api.Test
import org.nerve.boot.util.RSAProvider
import java.io.File
import java.io.FileWriter


/*
 * @project app-meta-server
 * @file    org.appmeta.module.worker.WorkerServiceTest
 * CREATE   2023年10月16日 18:40 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class WorkerServiceTest {

    /*
    公钥：MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDBxks5qB92y4iXI49WpKM+t8lwwsW1E4eql6tMCzF5wveMLRflNZcb4+sIK6A4J+5ZyDjc2e9lPxJ+Y1yAXLH7cc/COSJj+EfrgFuOQcIGE06wTPCAs85cu3QDWIFmPc0D/37X09lt5CbsxCJiaDf/+poAdv+qbwtqRCpsercdLQIDAQAB
    私钥：MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAMHGSzmoH3bLiJcjj1akoz63yXDCxbUTh6qXq0wLMXnC94wtF+U1lxvj6wgroDgn7lnIONzZ72U/En5jXIBcsftxz8I5ImP4R+uAW45BwgYTTrBM8ICzzly7dANYgWY9zQP/ftfT2W3kJuzEImJoN//6mgB2/6pvC2pEKmx6tx0tAgMBAAECgYAIF1APzcHWk4QWD4GONByu4zyxjSh1OaYKDQA1kigUNfxhKYbcZsLzAq7PLgcoIR62OAKL0jvJRftvNJXptDUoQIgmEBQnDFzBPSnQFjWodvJvwzRcprxW686WRV8xziTZfGCWIB1IXciPdfsQKAjRxTODbNAuqrriWizF/7nJ0QJBAOgg0Qt6tcEOIeCFq4xgw0XyHfYWz/wBIXwoK6GNi8lQJFthLLngNb9sBI1rPZAy3/s+JS9+GIjag8eWqliXdWUCQQDVs8BG8K8+TqPKfVEkcqUdE1laoKNgwQ4kclTLiWxoi7Bsflopavpk6ZDVg7msSZ85SV5b+8uIxsKOzsWwMxApAkEA5OevnYVVhFoeWB0YzSaCihA3MXzPfq/SyG+IjxhZF507LQ2HoIiUF/86AgcVv4Qb0dM3sjzDjvkE6KYPt6sr7QJBAKNW3ORcGtYY7YBcAKVHK4TpwSZQGhBd/x1EdiOMSlwuSQ7kFK4Loo93JsjMAiL5ssXqmkDcWFmW8iaNTPS8UuECQBvEFZ/CjBsrrZn/A+eXm50zUYf/MNb8xyv4ENAoFPxsuSucuGM4pXoQqiO1Mj/SiPFKin0QTSXGDAHlnRn53JY=
     */

    val service = WorkerService()
    val pubKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDBxks5qB92y4iXI49WpKM+t8lwwsW1E4eql6tMCzF5wveMLRflNZcb4+sIK6A4J+5ZyDjc2e9lPxJ+Y1yAXLH7cc/COSJj+EfrgFuOQcIGE06wTPCAs85cu3QDWIFmPc0D/37X09lt5CbsxCJiaDf/+poAdv+qbwtqRCpsercdLQIDAQAB"
    val priKey = "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAMHGSzmoH3bLiJcjj1akoz63yXDCxbUTh6qXq0wLMXnC94wtF+U1lxvj6wgroDgn7lnIONzZ72U/En5jXIBcsftxz8I5ImP4R+uAW45BwgYTTrBM8ICzzly7dANYgWY9zQP/ftfT2W3kJuzEImJoN//6mgB2/6pvC2pEKmx6tx0tAgMBAAECgYAIF1APzcHWk4QWD4GONByu4zyxjSh1OaYKDQA1kigUNfxhKYbcZsLzAq7PLgcoIR62OAKL0jvJRftvNJXptDUoQIgmEBQnDFzBPSnQFjWodvJvwzRcprxW686WRV8xziTZfGCWIB1IXciPdfsQKAjRxTODbNAuqrriWizF/7nJ0QJBAOgg0Qt6tcEOIeCFq4xgw0XyHfYWz/wBIXwoK6GNi8lQJFthLLngNb9sBI1rPZAy3/s+JS9+GIjag8eWqliXdWUCQQDVs8BG8K8+TqPKfVEkcqUdE1laoKNgwQ4kclTLiWxoi7Bsflopavpk6ZDVg7msSZ85SV5b+8uIxsKOzsWwMxApAkEA5OevnYVVhFoeWB0YzSaCihA3MXzPfq/SyG+IjxhZF507LQ2HoIiUF/86AgcVv4Qb0dM3sjzDjvkE6KYPt6sr7QJBAKNW3ORcGtYY7YBcAKVHK4TpwSZQGhBd/x1EdiOMSlwuSQ7kFK4Loo93JsjMAiL5ssXqmkDcWFmW8iaNTPS8UuECQBvEFZ/CjBsrrZn/A+eXm50zUYf/MNb8xyv4ENAoFPxsuSucuGM4pXoQqiO1Mj/SiPFKin0QTSXGDAHlnRn53JY="

    val localWorker = RemoteWorker("localhost").also { it.pubKey = pubKey }

    @Test
    fun call(){
        service.call(
            localWorker,
            WorkerTask("status", mapOf("name" to "集成显卡"))
        )
    }

    @Test
    fun runRobot(){
        service.call(
            localWorker,
            WorkerTask("start", mapOf(
                "page" to mapOf(F.AID to "DEMO", F.ID to "25", F.NAME to "演示 WEB-RPA （OSCHINA）"),
                "bean" to mapOf(
                    F.URL   to "https://www.oschina.net",
                    "code"  to """
                        let keyword = params.keyword.toLowerCase()
                        let texts = []
                        document.querySelectorAll(".headline,.tab-page").forEach(v=>{
                            v.querySelectorAll("a").forEach(a=>{
                                if(a.className.indexOf("title")>=0){
                                    let {text, href} = a
                                    if(text.toLowerCase().indexOf(keyword)>=0){
                                        texts.push(text)
                                        META.log("找到 ["+keyword+"] 的文章："+text)
                                    }
                                }
                            })
                        })

                        alert("数据获取完成")


                        META.notify("从开源中国（OSCHINA）首页咨询栏获取到关键字 "+keyword+" 相关文章 "+texts.length+" 篇，请查看日志获取详细的信息")
                        // META.data(texts)
                        META.finish("作业完成，共获取数据 "+texts.length+" 条")
                    """.trimIndent(),
                    "snapshot"  to false
                ),
                "params" to mapOf("keyword" to "java")
            ))
        )
    }

    @Test
    fun createRSAKey(){
        RSAProvider().also {
            println("公钥：${it.publicKey}")
            println("私钥：${it.privateKey}")

            val text = "Hello，集成显卡！Hello，集成显卡！Hello，集成显卡！Hello，集成显卡！Hello，集成显卡！Hello，集成显卡！Hello，集成显卡！Hello，集成显卡！"
            val miwen = it.encrypt(text)
            println("密文：${miwen}")
            println("解码：${it.decrypt(miwen)}")
        }
    }
}