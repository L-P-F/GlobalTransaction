package cn.distribute.aspect;

import cn.distribute.context.GTContext;
import cn.distribute.entity.TransactionResource;
import cn.distribute.enums.StatusEnum;
import cn.distribute.rpc.HTTPUtil;
import cn.distribute.rpc.SocketClient;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/*2024-04-17 14:35
 * Author: Aurora
 */
@Aspect
@Slf4j
public class GTAspect
{
    @Autowired
    private SocketClient socketClient;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Pointcut("@annotation(cn.distribute.anno.GlobalTransaction)")
    public void GTCut() {}

    @Pointcut("@annotation(cn.distribute.anno.BranchTransaction)")
    public void BTCut() {}

    @Around("GTCut()") //本身也是一个分支事务
    public Object GTStart(ProceedingJoinPoint point) throws Throwable
    {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        //开启事务
        TransactionStatus status = Objects.requireNonNull(transactionTemplate.getTransactionManager()).getTransaction(definition);
        //设置当前全局事务是否需要向服务器注册的请求
        GTContext.setWhetherFirstSend(new AtomicBoolean(true));

        String xid = UUID.randomUUID().toString();
        log.info("开始全局事务,{}", xid);
        GTContext.GTInit(xid, status);
        if(GTContext.getWhetherFirstSend().compareAndSet(true, false))
            HTTPUtil.saveBranch(xid);//判断是否已经注册过，注册过就不在发送注册请求

        Object result = point.proceed();

        //同步对接服务器,等待服务器通知commit OR rollback
        GTCommitOrRollback(StatusEnum.TRUE, status);

        return result;
    }

    @Around("BTCut()")
    public Object BTStart(ProceedingJoinPoint point) throws Throwable
    {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        //开启事务
        TransactionStatus status = Objects.requireNonNull(transactionTemplate.getTransactionManager()).getTransaction(definition);

        String xid = GTContext.getXid();
        if (xid != null)
        {
            MethodSignature ms = (MethodSignature) point.getSignature();
            log.info("分支事务开启：{}，隶属于全局事务：{}", ms.getMethod().getName(), xid);
            GTContext.BTInit(xid, status);
            HTTPUtil.saveBranch(xid);
        }

        Object result = point.proceed();

        if (xid != null)
            //异步对接服务器,等待服务器通知commit OR rollback
            BTCommitOrRollback(StatusEnum.TRUE, status, TransactionResource.copyTransactionResource());

        return result;
    }

    @AfterThrowing(pointcut = "GTCut()", throwing = "e")
    public void GTException(Throwable e)
    {
        log.error("全局事务异常：{},id：{}", e.getMessage(), GTContext.getXid());
        GTCommitOrRollback(StatusEnum.FALSE, GTContext.getBT().getTransactionStatus());
    }

    @AfterThrowing(pointcut = "BTCut()", throwing = "e")
    public void BTException(Throwable e)
    {
        log.error("分支事务异常：{}，隶属于全局事务：{}", e.getMessage(), GTContext.getXid());
        BTCommitOrRollback(StatusEnum.FALSE, GTContext.getBT().getTransactionStatus(), TransactionResource.copyTransactionResource());
    }


    private void GTCommitOrRollback(StatusEnum statusEnum, TransactionStatus status)
    {
        socketClient.GTTryToConnect(statusEnum.getMsg(), GTContext.getXid(), transactionTemplate, status);
    }

    private void BTCommitOrRollback(StatusEnum statusEnum, TransactionStatus status, TransactionResource transactionResource)
    {
        socketClient.BTTryToConnect(statusEnum.getMsg(), GTContext.getXid(), transactionTemplate, status, transactionResource);
    }
}
