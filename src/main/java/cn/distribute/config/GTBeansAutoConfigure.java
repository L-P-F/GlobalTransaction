package cn.distribute.config;

import cn.distribute.aspect.GTAspect;
import cn.distribute.interceptor.GTFeignReqReceive;
import cn.distribute.interceptor.GTFeignReqSend;
import cn.distribute.interceptor.SQLInterceptor;
import cn.distribute.properties.GTConfigurationProperties;
import cn.distribute.until.CommonUtil;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 2024-04-18 13:28
 * <p>Author: Aurora-LPF</p>
 * <p>自动配置类</p>
 */
@Configuration
@EnableConfigurationProperties(GTConfigurationProperties.class)
public class GTBeansAutoConfigure
{
    @Bean
    protected GTFeignReqSend feignReqSend()
    {
        return new GTFeignReqSend();
    }

    @Bean
    public GTFeignReqReceive feignReqReceive()
    {
        return new GTFeignReqReceive();
    }

    @Bean
    public GTAspect gtAspect()
    {
        return new GTAspect();
    }

    @Bean
    public SQLInterceptor sqlInterceptor()
    {
        return new SQLInterceptor();
    }

    @Bean
    public CommonUtil commonUtil(GTConfigurationProperties gtConfigurationProperties)
    {
        return new CommonUtil(gtConfigurationProperties);
    }
}
