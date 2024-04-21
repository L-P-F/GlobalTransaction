package cn.distribute.aspect;

import cn.distribute.context.GTContext;
import cn.distribute.rpc.HTTPUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.UUID;

/*2024-04-17 14:35
 * Author: Aurora
 */
@Aspect
@Slf4j
public class GTAspect
{
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
        // HTTPUtil.saveBranch(xid); todo 也没必要

        Object result = point.proceed();

        //HTTPUtil.saveBranch(xid); todo 感觉没必要，有待商榷

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
            //HTTPUtil.saveBranch(xid);  todo 感觉也没必要
        }

        Object result = point.proceed();

        if(xid != null)
            HTTPUtil.saveBranch(xid);

        return result;
    }

    @AfterThrowing(pointcut = "GTCut()", throwing = "e")
    public void GTException(JoinPoint point, Throwable e)
    {
        log.error("全局事务异常：{}", e.getMessage());
        log.info("全局事务ID：{}", GTContext.getXid());
    }

    @AfterThrowing(pointcut = "BTCut()", throwing = "e")
    public void BTException(JoinPoint point, Throwable e)
    {
        log.error("分支事务异常：{}", e.getMessage());
    }
}
