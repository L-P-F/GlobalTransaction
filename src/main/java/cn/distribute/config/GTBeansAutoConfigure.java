package cn.distribute.config;

import cn.distribute.aspect.GTAspect;
import cn.distribute.interceptor.GTFeignReqReceive;
import cn.distribute.interceptor.GTFeignReqSend;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/*2024-04-18 13:28
 * Author: Aurora
 */
@Configuration
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
    @ConditionalOnBean(DataSourceTransactionManager.class)
    public TransactionTemplate transactionTemplate(DataSourceTransactionManager transactionManager)
    {
        return new TransactionTemplate(transactionManager);
    }
}
