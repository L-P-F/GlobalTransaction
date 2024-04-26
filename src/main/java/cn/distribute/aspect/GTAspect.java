package cn.distribute.aspect;

import cn.distribute.config.GTSocketClientAutoConfigure;
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
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private TransactionTemplate transactionTemplate;

    @Pointcut("@annotation(cn.distribute.anno.GlobalTransaction)")
    public void GTCut()
    {
    }

    @Pointcut("@annotation(cn.distribute.anno.BranchTransaction)")
    public void BTCut()
    {
    }

    @Around("GTCut()") //本身也是一个分支事务
    public Object GTStart(ProceedingJoinPoint point) throws Throwable
    {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        //开启事务
        TransactionStatus status = Objects.requireNonNull(transactionTemplate.getTransactionManager()).getTransaction(definition);
        //设置当前全局事务是否需要向服务器注册的请求
        GTContext.setWhetherFirstSend(new AtomicBoolean(true));

        String xid = UUID.randomUUID().toString();
        GTContext.GTInit(xid, status);
        if (GTContext.getWhetherFirstSend().compareAndSet(true, false))
            HTTPUtil.saveBranch(xid);//判断是否已经注册过，注册过就不在发送注册请求
        log.info("开始全局事务,xid: {},执行顺序: {}", xid, GTContext.getBT().getExecuteOrder());

        Object result = point.proceed();

        //同步对接服务器,等待服务器通知commit OR rollback
        GTCommitOrRollback(StatusEnum.TRUE);
        GTContext.clear();//防止内存泄漏

        return result;
    }

    @Around("BTCut()")
    public Object BTStart(ProceedingJoinPoint point) throws Throwable
    {
        String xid = GTContext.getXid();
        if (xid != null)
        {
            DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
            //开启事务
            TransactionStatus status = Objects.requireNonNull(transactionTemplate.getTransactionManager()).getTransaction(definition);

            MethodSignature ms = (MethodSignature) point.getSignature();
            GTContext.BTInit(xid, status);
            HTTPUtil.saveBranch(xid);
            log.info("分支事务开启,方法名: {}，隶属于全局事务: {},执行顺序: {}", ms.getMethod().getName(), xid, GTContext.getBT().getExecuteOrder());
        }

        Object result = point.proceed();

        if (xid != null)
            //异步对接服务器,等待服务器通知commit OR rollback
            BTCommitOrRollback(StatusEnum.TRUE);

        return result;
    }

    @AfterThrowing(pointcut = "GTCut()", throwing = "e")
    public void GTException(Throwable e)
    {
        log.error("事务异常: {},隶属于全局事务: {},在全局事务中处于第{}位", e.getMessage(), GTContext.getXid(), GTContext.getBT().getExecuteOrder());
        log.error("开始向服务器推送【回滚】请求");
        GTCommitOrRollback(StatusEnum.FALSE);
        GTContext.clear();//防止内存泄漏
    }

    @AfterThrowing(pointcut = "BTCut()", throwing = "e")
    public void BTException(Throwable e)
    {
        log.error("事务异常: {},隶属于全局事务: {},在全局事务中处于第{}位", e.getMessage(), GTContext.getXid(), GTContext.getBT().getExecuteOrder());
        log.error("开始向服务器推送【回滚】请求");
        BTCommitOrRollback(StatusEnum.FALSE);
    }


    private void GTCommitOrRollback(StatusEnum statusEnum)
    {
        AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(GTSocketClientAutoConfigure.class);
        ac.getBean(SocketClient.class).GTTryToConnect(GTContext.getBT(), statusEnum.getMsg(), transactionTemplate);
    }

    private void BTCommitOrRollback(StatusEnum statusEnum)
    {
        AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(GTSocketClientAutoConfigure.class);
        ac.getBean(SocketClient.class).BTTryToConnect(GTContext.getBT(), statusEnum.getMsg(), transactionTemplate, TransactionResource.copyTransactionResource());
    }
}
