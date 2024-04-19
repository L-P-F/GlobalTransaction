package cn.distribute.interceptor;

import cn.distribute.context.GTContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;

import java.util.Properties;
/*2024-04-17 16:10
 * Author: Aurora
 */

@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class SQLInterceptor implements Interceptor
{
    Properties properties = null;

    @Override
    public Object intercept(Invocation invocation) throws Throwable
    {
        if(GTContext.getXid() == null)
            return invocation.proceed();//不拦截全局事务以外调用的sql语句
        Object[] args = invocation.getArgs();
        MappedStatement mappedStatement = (MappedStatement) args[0];
        Object parameter = args[1];
        System.out.println(mappedStatement.getBoundSql(parameter).getSql());
        return invocation.proceed();
    }

    /**
     * 生成MyBatis拦截器代理对象
     */
    @Override
    public Object plugin(Object target) {
        if(target instanceof Executor)
            // 调用插件
            return Plugin.wrap(target, this);
        return target;
    }

    /**
     * 设置插件属性（直接通过Spring的方式获取属性，所以这个方法一般也用不到）
     * 项目启动的时候数据就会被加载
     */
    @Override
    public void setProperties(Properties properties) {
        // 赋值成员变量，在其他方法使用
        this.properties = properties;
    }
}

