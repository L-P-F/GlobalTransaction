package cn.distribute.aspect;

import cn.distribute.context.GTContext;
import cn.distribute.enums.StatusEnum;
import cn.distribute.rpc.HTTPUtil;
import cn.distribute.rpc.SocketClient;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.UUID;

/*2024-04-17 14:35
 * Author: Aurora
 */
@Aspect
@Slf4j
public class GTAspect
{
    @Autowired
    private SocketClient socketClient;

    @Pointcut("@annotation(cn.distribute.anno.GlobalTransaction)")
    public void GTCut() {}

    @Pointcut("@annotation(cn.distribute.anno.BranchTransaction)")
    public void BTCut() {}

    @Around("GTCut()") //本身也是一个分支事务
    public Object GTStart(ProceedingJoinPoint point) throws Throwable
    {
        String xid = UUID.randomUUID().toString();
        log.info("开始全局事务,{}", xid);
        GTContext.GTInit(xid);

        Object result = point.proceed();

        socketClient.connectToServer(true);
        String message = socketClient.getLatestMessage();
        log.info(message);
        if(!Boolean.getBoolean(message))
           TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        return result;
    }

    @Around("BTCut()")
    public Object BTExecute(ProceedingJoinPoint point) throws Throwable
    {
        String xid = GTContext.getXid();
        if (xid != null)
        {
            MethodSignature ms = (MethodSignature) point.getSignature();
            log.info("分支事务开启：{}，隶属于全局事务：{}", ms.getMethod().getName(),xid);
            GTContext.BTInit(xid);
        }

        Object result = point.proceed();

        if(xid != null)
        {
            HTTPUtil.saveBranch(xid);
            socketClient.connectToServer(true);
            String message = socketClient.getLatestMessage();
            log.info(message);
            if(!Boolean.getBoolean(message))
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }

        return result;
    }

    @AfterThrowing(pointcut = "GTCut()", throwing = "e")
    public void GTException(JoinPoint point,Throwable e)
    {
        log.error("全局事务异常：{}", e.getMessage());
        log.info("全局事务ID：{}", GTContext.getXid());
        GTContext.getBT().setStatus(StatusEnum.ROLLBACK.getCode());
        HTTPUtil.saveBranch(GTContext.getXid());
        socketClient.connectToServer(false);
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
    }

    @AfterThrowing(pointcut = "BTCut()", throwing = "e")
    public void BTException(JoinPoint point,Throwable e)
    {
        log.error("分支事务异常：{}", e.getMessage());
        GTContext.getBT().setStatus(StatusEnum.ROLLBACK.getCode());
        HTTPUtil.saveBranch(GTContext.getXid());
        socketClient.connectToServer(false);
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
    }
}
