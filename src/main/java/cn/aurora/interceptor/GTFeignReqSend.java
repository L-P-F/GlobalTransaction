package cn.aurora.interceptor;

import cn.aurora.context.GTContext;
import cn.aurora.enums.ParamsEnum;
import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * 2024-04-18 12:15
 * <p>Author: Aurora-LPF</p>
 * <p>拦截feign向外发送的http请求并进行封装数据</p>
 */
public class GTFeignReqSend implements RequestInterceptor
{
    @Override
    public void apply(RequestTemplate template)
    {
        String xid = GTContext.getXid(); //只有通过GlobalTransaction注解的方法才有这个xid,如果xid为null,代表本次向外发送的feign请求不属于全局事务
        if (xid != null)
            template.header(ParamsEnum.XID.getValue(), xid);
    }
}
