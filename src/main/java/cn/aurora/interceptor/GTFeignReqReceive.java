package cn.aurora.interceptor;

import cn.aurora.context.GTContext;
import cn.aurora.enums.ParamsEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 2024-04-18 12:15
 * <p>Author: Aurora-LPF</p>
 * <p>拦截发送进来的feign请求,拦截器需要用户在自己的项目中注册,拦截所有路径即可</p>
 */
public class GTFeignReqReceive implements HandlerInterceptor
{
    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object o)
    {
        String xid = req.getHeader(ParamsEnum.XID.getValue());
        GTContext.setXid(xid);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse rep, Object o, Exception e)
    {
        //最后编写本地线程变量的remove，防止内存泄漏
        GTContext.clear();
    }
}