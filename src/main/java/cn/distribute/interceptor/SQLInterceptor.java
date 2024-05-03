package cn.distribute.interceptor;

import cn.distribute.context.GTContext;
import cn.distribute.entity.database.UndoExecutorFactory;
import cn.distribute.entity.database.undoExecutor.AbstractUndoExecutor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.sql.Connection;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * 2024-04-17 16:10
 * <p>Author: Aurora-LPF</p>
 * <p>mybatis sql拦截器</p>
 */

@Slf4j
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class SQLInterceptor implements Interceptor
{
    Properties properties = null;

    @Override
    public Object intercept(Invocation invocation) throws Throwable
    {
        if (GTContext.getXid() == null)
            return invocation.proceed();//拦截开启全局事务以外调用的sql语句，直接放行，不做任何处理
        String sql = resolveSqlWithParameters(invocation);

        //try-with-resource进行执行,可以自动释放connection连接,不需要手动close
        try(Connection connection = ((MappedStatement) invocation.getArgs()[0]).getConfiguration().getEnvironment().getDataSource().getConnection())
        {
            AbstractUndoExecutor undoExecutor = UndoExecutorFactory.getUndoExecutor(((MappedStatement) invocation.getArgs()[0]).getSqlCommandType());
            log.info("SQL:==> {}", sql);

            // 初始化undoLog对象并绑定前置镜像
            GTContext.setSQLUndoLog(undoExecutor.buildSQLUndoLog(sql, connection, getTableName(sql)));

            Object result = invocation.proceed();


            // 绑定后置镜像
            undoExecutor.bindAfterImage(sql, GTContext.getLastSQLUndoLog(), connection);
            return result;
        }
    }

    public String getTableName(String sql)
    {
        String regex = "\\b(?:INSERT\\s+INTO\\s+(\\w+)|DELETE\\s+FROM\\s+(\\w+)|UPDATE\\s+(\\w+)\\s+SET)\\b";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);
        if (matcher.find())
            for (int i = 1; i <= 3; i++)
                if (matcher.group(i) != null)
                    return matcher.group(i);
        return null;
    }

    @SneakyThrows
    private String resolveSqlWithParameters(Invocation invocation)
    {
        Object[] args = invocation.getArgs();
        MappedStatement mappedStatement = (MappedStatement) args[0];
        Object parameter = args[1];

        BoundSql boundSql = mappedStatement.getBoundSql(parameter);

        Configuration configuration = mappedStatement.getConfiguration();
        //获取参数对象
        Object parameterObject = boundSql.getParameterObject();
        //获取当前的sql语句有绑定的所有parameterMapping属性
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        //去除空格
        String sql = boundSql.getSql().replaceAll("[\\s]+", " ");
        if (parameterMappings.size() > 0 && parameterObject != null)
        {
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
         /*如果参数满足：org.apache.ibatis.type.TypeHandlerRegistry#hasTypeHandler(java.lang.Class<?>)
                    org.apache.ibatis.type.TypeHandlerRegistry#TYPE_HANDLER_MAP
                    * 即是不是属于注册类型(TYPE_HANDLER_MAP...等/有没有相应的类型处理器)
                     * */

            if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass()))
                sql = sql.replaceFirst("\\?", getParameterValue(parameterObject));
            else
            {
                //装饰器，可直接操作属性值 ---》 以parameterObject创建装饰器
                //MetaObject 是 Mybatis 反射工具类，通过 MetaObject 获取和设置对象的属性值
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                //循环 parameterMappings 所有属性
                for (ParameterMapping parameterMapping : parameterMappings)
                {
                    //获取property属性
                    String propertyName = parameterMapping.getProperty();
                    //是否声明了propertyName的属性和get方法
                    if (metaObject.hasGetter(propertyName))
                    {
                        Object obj = metaObject.getValue(propertyName);
                        sql = sql.replaceFirst("\\?", getParameterValue(obj));
                    } else if (boundSql.hasAdditionalParameter(propertyName))
                    {
                        //判断是不是sql的附加参数
                        Object obj = boundSql.getAdditionalParameter(propertyName);
                        sql = sql.replaceFirst("\\?", getParameterValue(obj));
                    }
                }
            }
        }
        return sql;
    }

    private String getParameterValue(Object obj)
    {
        String value;
        if (obj instanceof String)
            value = "'" + obj + "'";
        else
        {
            if (obj != null)
                value = obj.toString();
            else
                value = "";
        }
        return value;
    }

    /**
     * 生成MyBatis拦截器代理对象
     */
    @Override
    public Object plugin(Object target)
    {
        if (target instanceof Executor)
            // 调用插件
            return Plugin.wrap(target, this);
        return target;
    }

    /**
     * 设置插件属性（直接通过Spring的方式获取属性，所以这个方法一般也用不到）
     * 项目启动的时候数据就会被加载
     */
    @Override
    public void setProperties(Properties properties)
    {
        // 赋值成员变量，在其他方法使用
        this.properties = properties;
    }
}

