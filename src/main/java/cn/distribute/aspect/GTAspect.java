package cn.distribute.aspect;

import cn.distribute.context.GTContext;
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
    public void BTCut(){}

    @Around("GTCut()") //本身也是一个分支事务
    public Object GTStart(ProceedingJoinPoint point) throws Throwable
    {
        String xid = UUID.randomUUID().toString();
        log.info("开始全局事务,{}",xid);
        GTContext.GTInit(xid);
        return point.proceed();
    }

    @Around("BTCut()")
    public Object BTExecute(ProceedingJoinPoint point) throws Throwable
    {
        if(GTContext.getXid() != null)
        {
            MethodSignature ms = (MethodSignature) point.getSignature();
            String xid = GTContext.getXid();
            log.info("执行分支事务：{}", ms.getMethod().getName());
            log.info("隶属于全局事务：{}" , xid);
        }
        return point.proceed();
    }

    @AfterThrowing(pointcut = "GTCut()", throwing = "e")
    public void GTException(JoinPoint point, Throwable e)
    {
        log.error("全局事务异常：{}",e.getMessage());
        log.info("全局事务ID：{}", GTContext.getXid());
    }

    @AfterThrowing(pointcut = "BTCut()", throwing = "e")
    public void BTException(JoinPoint point, Throwable e)
    {
        log.error("分支事务异常：{}",e.getMessage());
    }
}
