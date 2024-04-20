package cn.distribute.aspect;

import cn.distribute.context.GTContext;
import cn.distribute.entity.BT;
import cn.distribute.entity.Result;
import cn.distribute.enums.HTTPEnum;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.io.IOException;
import java.util.List;
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
        saveBranch(xid);
        return point.proceed();
    }

    @Around("BTCut()")
    public Object BTExecute(ProceedingJoinPoint point) throws Throwable
    {
        if (GTContext.getXid() != null)
        {
            String xid = GTContext.getXid();
            getBranch(xid);
            MethodSignature ms = (MethodSignature) point.getSignature();
            log.info("执行分支事务：{}", ms.getMethod().getName());
            log.info("隶属于全局事务：{}", xid);
            GTContext.appendBT(xid);
            saveBranch(xid);

        }
        return point.proceed();
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

    private void saveBranch(String xid)
    {
        String jsonBody = JSON.toJSONString(GTContext.GTList.get(xid));
        log.info("保存分支事务请求体: {}", jsonBody);
        try (CloseableHttpClient httpClient = HttpClients.createDefault())
        {
            HttpPost httpPost = new HttpPost(HTTPEnum.SAVE.getUrl());
            StringEntity entity = new StringEntity(jsonBody, ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);
            try (CloseableHttpResponse response = httpClient.execute(httpPost))
            {
                HttpEntity responseEntity = response.getEntity();
                if (responseEntity != null)
                {
                    String responseBody = EntityUtils.toString(responseEntity);
                    log.info("保存分支事务响应体: {}", responseBody);
                } else
                    log.error("保存没有响应.");
            }
        } catch (IOException e)
        {
            log.error("保存Error getting data: {}", e.getMessage());
        }
    }

    private void getBranch(String xid)
    {
        try (CloseableHttpClient httpClient = HttpClients.createDefault())
        {
            HttpGet httpGet = new HttpGet(HTTPEnum.GET.getUrl() + xid);
            try (CloseableHttpResponse response = httpClient.execute(httpGet))
            {
                HttpEntity responseEntity = response.getEntity();
                if (responseEntity != null)
                {
                    String responseBody = EntityUtils.toString(responseEntity);
                    log.info("拉取分支事务响应体: {}", responseBody);
                    Result result = JSON.parseObject(responseBody, Result.class);
                    List<BT> btList = JSON.parseArray(result.getContent().toString(), BT.class);
                    GTContext.GTList.put(xid,btList);
                } else
                    log.error("拉取没有响应.");
            }
        } catch (IOException e)
        {
            log.error("拉取Error getting data: {}", e.getMessage());
        }
    }
}
