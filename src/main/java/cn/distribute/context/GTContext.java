package cn.distribute.context;

import cn.distribute.entity.BT;
import cn.distribute.enums.StatusEnum;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/*2024-04-18 10:59
 * Author: Aurora
 */
public class GTContext
{
    private static final ThreadLocal<BT> CURRENT_BT = new ThreadLocal<>();
    private static final ThreadLocal<String> XID_THREAD_LOCAL = new ThreadLocal<>();
    public static final ConcurrentHashMap<String, List<BT>> GTList = new ConcurrentHashMap<>();

    public static void setXid(String xid)
    {
        XID_THREAD_LOCAL.set(xid);
    }

    public static String getXid()
    {
        return XID_THREAD_LOCAL.get();
    }

    public static void setBT(BT bt)
    {
        CURRENT_BT.set(bt);
    }

    public static BT getBT()
    {
        return CURRENT_BT.get();
    }

    public static void GTInit(String xid)
    {
        setXid(xid);
        BTInit(xid);
    }

    public static void BTInit(String xid)
    {
        BT bt = BT.builder()
                .xid(xid)
                .status(StatusEnum.START.getCode())
                .sqlData(new LinkedHashSet<>())
                .build();
        setBT(bt); //当前线程本地BT改为当前BT
    }
}
