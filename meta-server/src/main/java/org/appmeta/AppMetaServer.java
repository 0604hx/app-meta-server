package org.appmeta;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableCaching
@EnableScheduling
@SpringBootApplication
@ComponentScan({"org.appmeta", "org.nerve"})
@MapperScan({"org.nerve.boot.module", "org.appmeta.domain", "org.appmeta.module"})
public class AppMetaServer {

    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        context = SpringApplication.run(AppMetaServer.class, args);
    }

    /**
     * 重启上下文
     */
    public static void restart(){
        ApplicationArguments args = context.getBean(ApplicationArguments.class);
        Thread thread = new Thread(() -> {
            context.close();
            try {
                Thread.sleep(1000);
            }
            catch (Exception ignored){}
            context = SpringApplication.run(AppMetaServer.class, args.getSourceArgs());
        });
        // 设置非守护线程
        thread.setDaemon(false);
        thread.start();
    }
}
