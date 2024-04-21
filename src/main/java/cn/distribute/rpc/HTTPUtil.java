package cn.distribute.rpc;

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

import java.io.IOException;
import java.util.List;

/*2024-04-20 11:14
 * Author: Aurora
 */
@Slf4j
public class HTTPUtil
{
    public static void saveBranch(String xid)
    {
        String jsonBody = JSON.toJSONString(GTContext.getBT());
        log.info("保存分支事务请求体: {}", jsonBody);
        try (CloseableHttpClient httpClient = HttpClients.createDefault())
        {
            HttpPost httpPost = new HttpPost(HTTPEnum.SAVE.getUrl() + xid);
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

    public static void getBranch(String xid)
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
                    log.info("拉取分支事务: {}", responseBody);
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
