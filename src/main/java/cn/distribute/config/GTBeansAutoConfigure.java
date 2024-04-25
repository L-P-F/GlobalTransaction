package cn.distribute.config;

import cn.distribute.aspect.GTAspect;
import cn.distribute.interceptor.GTFeignReqReceive;
import cn.distribute.interceptor.GTFeignReqSend;
import cn.distribute.rpc.SocketClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.support.TransactionTemplate;

/*2024-04-18 13:28
 * Author: Aurora
 */
@EnableAsync
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
    @Scope("prototype")//每次创建新的socketClient防止并发出现数据安全问题
    public SocketClient socketClient()
    {
        return new SocketClient();
    }

    @Bean
    @ConditionalOnBean(DataSourceTransactionManager.class)
    public TransactionTemplate transactionTemplate(DataSourceTransactionManager transactionManager)
    {
        return new TransactionTemplate(transactionManager);
    }
}
