package cn.distribute.config;

import cn.distribute.aspect.GTAspect;
import cn.distribute.interceptor.GTFeignReqReceive;
import cn.distribute.interceptor.GTFeignReqSend;
import cn.distribute.interceptor.SQLInterceptor;
import cn.distribute.rpc.SocketClient;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
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
    protected SQLInterceptor sqlInterceptor()
    {
        return new SQLInterceptor();
    }

    @Bean
    @ConditionalOnBean(SQLInterceptor.class)
    public Boolean setAutoFillValueInterceptor(SqlSessionFactory sqlSessionFactory, SQLInterceptor sqlInterceptor)
    {
        org.apache.ibatis.session.Configuration configuration = sqlSessionFactory.getConfiguration();
        configuration.addInterceptor(sqlInterceptor);
        return Boolean.TRUE;
    }

    @Bean
    public GTAspect gtAspect()
    {
        return new GTAspect();
    }

    @Bean
    public SocketClient socketClient()
    {
        return new SocketClient();
    }

    @Bean
    @ConditionalOnBean(PlatformTransactionManager.class)
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager)
    {
        return new TransactionTemplate(transactionManager);
    }
}
