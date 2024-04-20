package cn.distribute.context;

import cn.distribute.entity.BT;
import cn.distribute.enums.StatusEnum;

import java.util.ArrayList;
import java.util.HashSet;
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
        List<BT> BTList = new ArrayList<>();
        BT bt = BT.builder()
                .xid(xid)
                .bid(xid + "-0")
                .status(StatusEnum.START.getCode())
                .sqlData(new HashSet<>())
                .build();
        setBT(bt);
        BTList.add(bt);
        GTList.put(xid,BTList);
    }

    public static void appendBT(String xid)
    {
        List<BT> BTList = GTList.get(xid);
        BT bt = BT.builder()
                .xid(xid)
                .bid(xid + "-" + BTList.size())
                .status(StatusEnum.START.getCode())
                .executeOrder(BTList.size())
                .sqlData(new HashSet<>())
                .build();
        setBT(bt);
        BTList.add(bt);
    }
}
