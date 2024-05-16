package cn.aurora.config;

import cn.aurora.aspect.GTAspect;
import cn.aurora.interceptor.GTFeignReqReceiveInterceptor;
import cn.aurora.interceptor.GTFeignReqSendInterceptor;
import cn.aurora.interceptor.SQLInterceptor;
import cn.aurora.properties.GTConfigurationProperties;
import cn.aurora.until.CommonUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
    protected GTFeignReqSendInterceptor feignReqSend()
    {
        return new GTFeignReqSendInterceptor();
    }

    @Bean
    public GTFeignReqReceiveInterceptor feignReqReceive()
    {
        return new GTFeignReqReceiveInterceptor();
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

    @Bean
    @ConditionalOnBean(DataSourceTransactionManager.class)
    public TransactionTemplate transactionTemplate(DataSourceTransactionManager dataSourceTransactionManager)
    {
        return new TransactionTemplate(dataSourceTransactionManager);
    }
}
