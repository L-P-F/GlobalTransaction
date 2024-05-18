package cn.aurora.aspect;

import cn.aurora.autoConfig.GTSocketClientAutoConfigure;
import cn.aurora.context.GTContext;
import cn.aurora.enums.StatusEnum;
import cn.aurora.rpc.HTTPClient;
import cn.aurora.rpc.SocketClient;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

/**
 * 2024-04-17 14:35
 * <p>Author: Aurora-LPF</p>
 * <P>core:切面拦截所有标记了GT,BT注解的方法,手动开启事务</P>
 */
@Aspect
@Slf4j
public class GTAspect
{
    @Autowired
    private DataSource dataSource;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Pointcut("@annotation(cn.aurora.anno.GlobalTransaction)")
    public void GTCut()
    {
    }

    @Pointcut("@annotation(cn.aurora.anno.BranchTransaction)")
    public void BTCut()
    {
    }

    @Around("GTCut()") //本身也是一个分支事务
    public Object GTStart(ProceedingJoinPoint point) throws Throwable
    {
        if (!GTContext.getWhetherFirstExecute().compareAndSet(true, false)) // 判断当前线程是否是第一次执行带有GT注解的方法
            return point.proceed(); // 不是 | 说明全局事务已经开启,直接执行,不进行增强
        else if (GTContext.getXid() != null) // 判断xid是否存在
            return BTStart(point); // 存在 | 说明本次调用属于被其他开启GT的方法进行远程调用,直接降级为BT参与到全局事务中

        String xid = UUID.randomUUID().toString();
        GTContext.GTInit(xid);
        HTTPClient.saveBranch(xid);
        log.info("开始全局事务,xid: {},执行顺序: {}", xid, GTContext.getBT().getExecuteOrder());

        Object result = point.proceed();

        //同步对接服务器,等待服务器通知commit OR rollback
        GTCommitOrRollback(StatusEnum.TRUE, StatusEnum.NONE_EXCEPTION);
        GTContext.clear();//防止内存泄漏

        return result;
    }

    @Around("BTCut()")
    public Object BTStart(ProceedingJoinPoint point) throws Throwable
    {
        String xid = GTContext.getXid();
        if (xid != null)
        {
            MethodSignature ms = (MethodSignature) point.getSignature();
            GTContext.BTInit(xid);
            HTTPClient.saveBranch(xid);
            log.info("分支事务开启,方法名: {}，隶属于全局事务: {},执行顺序: {}", ms.getMethod().getName(), xid, GTContext.getBT().getExecuteOrder());
        } else
        {
            DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
            TransactionStatus status = Objects.requireNonNull(transactionTemplate.getTransactionManager()).getTransaction(definition);
            GTContext.CTInit(status);
        }

        Object result = point.proceed();

        if (xid != null)
            //异步对接服务器,等待服务器通知commit OR rollback
            BTCommitOrRollback(StatusEnum.TRUE, StatusEnum.NONE_EXCEPTION);
        else
            transactionTemplate.getTransactionManager().commit(GTContext.getBT().getTransactionStatus());
        GTContext.clear(); //防止内存泄漏

        return result;
    }

    @AfterThrowing(pointcut = "GTCut()", throwing = "e")
    public void GTException(Throwable e)
    {
        log.error("事务异常: {},隶属于全局事务: {},在全局事务中处于第{}位,开始向服务器推送【回滚】请求", e.getMessage(), GTContext.getXid(), GTContext.getBT().getExecuteOrder());
        if (e instanceof SQLException || e instanceof DuplicateKeyException)
            GTCommitOrRollback(StatusEnum.FALSE, StatusEnum.SQL_EXCEPTION);
        else GTCommitOrRollback(StatusEnum.FALSE, StatusEnum.SERVER_EXCEPTION);
        GTContext.clear();//防止内存泄漏
    }

    @AfterThrowing(pointcut = "BTCut()", throwing = "e")
    public void BTException(Throwable e)
    {
        if (GTContext.getXid() == null)
        {
            Objects.requireNonNull(transactionTemplate.getTransactionManager()).rollback(GTContext.getBT().getTransactionStatus());
            return;
        }
        log.error("事务异常: {},隶属于全局事务: {},在全局事务中处于第{}位,开始向服务器推送【回滚】请求", e.getMessage(), GTContext.getXid(), GTContext.getBT().getExecuteOrder());
        if (e instanceof SQLException || e instanceof DuplicateKeyException)
            BTCommitOrRollback(StatusEnum.FALSE, StatusEnum.SQL_EXCEPTION);
        else BTCommitOrRollback(StatusEnum.FALSE, StatusEnum.SERVER_EXCEPTION);
        GTContext.clear(); //防止内存泄漏
    }


    private void GTCommitOrRollback(StatusEnum statusEnum, StatusEnum exceptionEnum)
    {
        AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(GTSocketClientAutoConfigure.class);
        ac.getBean(SocketClient.class).GTTryToConnect(GTContext.getBT(), statusEnum.getMsg(), dataSource, GTContext.getSQLUndoLogs(), exceptionEnum);
    }

    private void BTCommitOrRollback(StatusEnum statusEnum, StatusEnum exceptionEnum)
    {
        AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(GTSocketClientAutoConfigure.class);
        ac.getBean(SocketClient.class).BTTryToConnect(GTContext.getBT(), statusEnum.getMsg(), dataSource, GTContext.getSQLUndoLogs(), exceptionEnum);
    }
}
