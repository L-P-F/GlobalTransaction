package cn.distribute.config;

import cn.distribute.rpc.SocketClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/*2024-04-25 18:42
 * Author: Aurora
 */
@Configuration
@EnableAsync
public class GTSocketClientAutoConfigure
{
    @Bean
    @Scope("prototype")//每次创建新的socketClient防止并发出现数据安全问题
    public SocketClient socketClient()
    {
        return new SocketClient();
    }

    @Bean(value = "asyncTaskExecutor")
    public Executor asyncTaskExecutor()
    {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10); // 设置核心线程池大小
        executor.setMaxPoolSize(20); // 设置最大线程池大小
        executor.setQueueCapacity(100); // 设置队列容量
        executor.setThreadNamePrefix("Async-"); // 设置线程名前缀
        executor.initialize(); // 初始化线程池
        return executor;
    }
}
