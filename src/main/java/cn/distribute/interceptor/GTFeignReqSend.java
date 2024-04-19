package cn.distribute.interceptor;

import cn.distribute.context.GTContext;
import cn.distribute.enums.ParamsEnum;
import feign.RequestInterceptor;
import feign.RequestTemplate;

/*2024-04-18 12:15
 * Author: Aurora
 * 拦截feign向外发送的http请求并进行封装数据
 */
public class GTFeignReqSend implements RequestInterceptor
{
    @Override
    public void apply(RequestTemplate template)
    {
        template.header(ParamsEnum.XID.getValue(), GTContext.getXid());
    }
}
