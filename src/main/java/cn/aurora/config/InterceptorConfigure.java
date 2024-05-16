package cn.aurora.config;

import cn.aurora.interceptor.GTFeignReqReceiveInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/*
 * 2024-05-16 19:46
 * Author: Aurora
 * 注入http请求拦截器
 */
public class InterceptorConfigure implements WebMvcConfigurer
{
    private final GTFeignReqReceiveInterceptor feignReqReceiveInterceptor;

    public InterceptorConfigure(GTFeignReqReceiveInterceptor feignReqReceiveInterceptor)
    {
        this.feignReqReceiveInterceptor = feignReqReceiveInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(feignReqReceiveInterceptor)
                .addPathPatterns("/**");
    }
}
