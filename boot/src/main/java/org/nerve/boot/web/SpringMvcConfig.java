package org.nerve.boot.web;

import jakarta.annotation.Resource;
import org.nerve.boot.web.auth.AuthConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class SpringMvcConfig implements WebMvcConfigurer {
    private static final int ONE_DAY = 24 * 60 * 60;
    private static final int TEN_DAY = 10 * ONE_DAY;

    private final Logger logger = LoggerFactory.getLogger(SpringMvcConfig.class);

    @Autowired(required = false)
    List<HandlerInterceptor> interceptorList;

    @Resource
    AuthConfig authConfig;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if(interceptorList!=null){
            for(HandlerInterceptor interceptor:interceptorList){
                registry.addInterceptor(interceptor).excludePathPatterns(authConfig.getPopularUrls());
                if(logger.isDebugEnabled())
                    logger.debug("[INTERCEPTOR] 添加拦截器 {} EXCLUDES={}", interceptor, authConfig.getPopularUrls());
            }
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
       /*
		设置图片资源的缓存 默认10天缓存
		 */
        if(authConfig.isAttach()){
            logger.info("[静态资源] 对外开放 /{}/** 的静态资源访问（如需关闭 nerve.auth.attach=false）", authConfig.getAttachDir());
            registry.addResourceHandler("/"+authConfig.getAttachDir()+"/**")
                    .addResourceLocations("file:attach/")
                    .setCachePeriod(TEN_DAY)
                    .setCacheControl(CacheControl.maxAge(10, TimeUnit.DAYS).cachePublic())
                    .resourceChain(true)
            ;
        }
    }
}
