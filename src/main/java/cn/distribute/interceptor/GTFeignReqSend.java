package cn.distribute.interceptor;

import cn.distribute.context.GTContext;
import cn.distribute.enums.ParamsEnum;
import cn.distribute.rpc.HTTPUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;

import java.util.concurrent.atomic.AtomicBoolean;

/*2024-04-18 12:15
 * Author: Aurora
 * 拦截feign向外发送的http请求并进行封装数据
 */
public class GTFeignReqSend implements RequestInterceptor
{
    private final AtomicBoolean first = new AtomicBoolean(true);
    @Override
    public void apply(RequestTemplate template)
    {
        String xid = GTContext.getXid(); //只有通过GlobalTransaction注解的方法才有这个xid,如果xid为null,代表本次向外发送的feign请求不属于全局事务
        if(xid != null)
        {
            template.header(ParamsEnum.XID.getValue(), xid);
            if(first.compareAndSet(true, false))
                HTTPUtil.saveBranch(xid);//每拦截到一次向外发送的feign请求，就向服务器申请刷新一次xid全局事务的分支事务状态
        }
    }
}
