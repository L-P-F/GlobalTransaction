package cn.distribute.context;

import cn.distribute.entity.BT;
import cn.distribute.enums.StatusEnum;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.transaction.TransactionStatus;

import java.util.LinkedHashSet;

/*2024-04-18 10:59
 * Author: Aurora
 */
public class GTContext
{
    private static final ThreadLocal<BT> CURRENT_BT = new ThreadLocal<>();
    private static final ThreadLocal<String> XID_THREAD_LOCAL = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> TRANSACTION_COMPLETED = new ThreadLocal<>();
    private static final ThreadLocal<HikariDataSource> dataSourceThreadLocal = new ThreadLocal<>();

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

    public static void setTransactionCompleted(Boolean status)
    {
        TRANSACTION_COMPLETED.set(status);
    }

    public static Boolean getTransactionCompleted()
    {
        return TRANSACTION_COMPLETED.get();
    }

    public static void setDataSource(HikariDataSource dataSource)
    {
        dataSourceThreadLocal.set(dataSource);
    }

    public static HikariDataSource getDataSource()
    {
        return dataSourceThreadLocal.get();
    }

    public static void remove()
    {
        CURRENT_BT.remove();
        XID_THREAD_LOCAL.remove();
        TRANSACTION_COMPLETED.remove();
        dataSourceThreadLocal.remove();
    }

    public static void GTInit(String xid,TransactionStatus status)
    {
        setXid(xid);
        BTInit(xid,status);
    }

    public static void BTInit(String xid, TransactionStatus status)
    {
        BT bt = BT.builder()
                .xid(xid)
                .status(StatusEnum.START.getCode())
                .transactionStatus(status)
                .sqlData(new LinkedHashSet<>())
                .build();
        setBT(bt); //当前线程本地BT改为当前BT
    }
}
