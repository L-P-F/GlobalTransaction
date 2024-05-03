package cn.distribute.aspect;

import cn.distribute.config.GTSocketClientAutoConfigure;
import cn.distribute.context.GTContext;
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
import org.springframework.dao.DuplicateKeyException;

import javax.sql.DataSource;
import java.sql.SQLException;
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
    private DataSource dataSource;

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
        GTContext.setWhetherFirstSend(new AtomicBoolean(true));

        String xid = UUID.randomUUID().toString();
        GTContext.GTInit(xid);
        if (GTContext.getWhetherFirstSend().compareAndSet(true, false))
            HTTPUtil.saveBranch(xid);//判断是否已经注册过，注册过就不在发送注册请求
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
            HTTPUtil.saveBranch(xid);
            log.info("分支事务开启,方法名: {}，隶属于全局事务: {},执行顺序: {}", ms.getMethod().getName(), xid, GTContext.getBT().getExecuteOrder());
        } else
            GTContext.CTInit();

        Object result = point.proceed();

        if (xid != null)
            //异步对接服务器,等待服务器通知commit OR rollback
            BTCommitOrRollback(StatusEnum.TRUE, StatusEnum.NONE_EXCEPTION);

        return result;
    }

    @AfterThrowing(pointcut = "GTCut()", throwing = "e")
    public void GTException(Throwable e)
    {
        log.error("事务异常: {},隶属于全局事务: {},在全局事务中处于第{}位", e.getMessage(), GTContext.getXid(), GTContext.getBT().getExecuteOrder());
        log.error("开始向服务器推送【回滚】请求");
        if (e instanceof SQLException || e instanceof DuplicateKeyException)
            GTCommitOrRollback(StatusEnum.FALSE, StatusEnum.SQL_EXCEPTION);
        else GTCommitOrRollback(StatusEnum.FALSE, StatusEnum.SERVER_EXCEPTION);
        GTContext.clear();//防止内存泄漏
    }

    @AfterThrowing(pointcut = "BTCut()", throwing = "e")
    public void BTException(Throwable e)
    {
        log.error("事务异常: {},隶属于全局事务: {},在全局事务中处于第{}位", e.getMessage(), GTContext.getXid(), GTContext.getBT().getExecuteOrder());
        log.error("开始向服务器推送【回滚】请求");
        if (e instanceof SQLException || e instanceof  DuplicateKeyException)
            BTCommitOrRollback(StatusEnum.FALSE, StatusEnum.SQL_EXCEPTION);
        else BTCommitOrRollback(StatusEnum.FALSE, StatusEnum.SERVER_EXCEPTION);
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
