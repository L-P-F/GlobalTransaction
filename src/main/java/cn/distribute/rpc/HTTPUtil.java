package cn.distribute.rpc;

import cn.distribute.context.GTContext;
import cn.distribute.enums.ReqPathEnum;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

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
            HttpPost httpPost = new HttpPost(ReqPathEnum.HTTP_SAVE.getUrl() + xid);
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
}
