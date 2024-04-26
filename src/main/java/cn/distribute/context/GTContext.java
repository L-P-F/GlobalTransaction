package cn.distribute.context;

import cn.distribute.entity.BT;
import cn.distribute.enums.StatusEnum;
import org.springframework.transaction.TransactionStatus;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 2024-04-18 10:59
 * <p>Author: Aurora-LPF</p>
 */
public class GTContext
{
    private static final ThreadLocal<BT> CURRENT_BT = new ThreadLocal<>();
    private static final ThreadLocal<String> XID_THREAD_LOCAL = new ThreadLocal<>();
    private static final ThreadLocal<AtomicBoolean> WHETHER_FIRST_SEND_THREAD_LOCAL = new ThreadLocal<>();

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

    public static void setWhetherFirstSend(AtomicBoolean firstSend)
    {
        WHETHER_FIRST_SEND_THREAD_LOCAL.set(firstSend);
    }

    public static AtomicBoolean getWhetherFirstSend()
    {
        return WHETHER_FIRST_SEND_THREAD_LOCAL.get();
    }

    public static void clear()
    {
        CURRENT_BT.remove();
        XID_THREAD_LOCAL.remove();
        WHETHER_FIRST_SEND_THREAD_LOCAL.remove();
    }

    public static void GTInit(String xid, TransactionStatus status)
    {
        setXid(xid);
        BTInit(xid, status);
    }

    public static void BTInit(String xid, TransactionStatus status)
    {
        BT bt = BT.builder()
                .xid(xid)
                .status(StatusEnum.START.getCode())
                .transactionStatus(status)
                .build();
        setBT(bt); //当前线程本地BT改为当前BT
    }
}
