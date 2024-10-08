package cn.aurora.context;

import cn.aurora.entity.BT;
import cn.aurora.entity.database.entity.SQLUndoLog;
import cn.aurora.enums.StatusEnum;
import org.springframework.transaction.TransactionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 2024-04-18 10:59
 * <p>Author: Aurora-LPF</p>
 */
public class GTContext
{
    private static final ThreadLocal<BT> CURRENT_BT = new ThreadLocal<>();
    private static final ThreadLocal<String> XID = new ThreadLocal<>();
    private static final ThreadLocal<AtomicBoolean> WHETHER_FIRST_EXECUTE = ThreadLocal.withInitial(() -> new AtomicBoolean(true));
    private static final ThreadLocal<List<SQLUndoLog>> SQL_UNDO_LOG = new ThreadLocal<>();

    public static void setXid(String xid)
    {
        XID.set(xid);
    }

    public static String getXid()
    {
        return XID.get();
    }

    private static void setBT(BT bt)
    {
        CURRENT_BT.set(bt);
    }

    public static BT getBT()
    {
        return CURRENT_BT.get();
    }

    public static AtomicBoolean getWhetherFirstExecute()
    {
        return WHETHER_FIRST_EXECUTE.get();
    }

    public static void setSQLUndoLog(SQLUndoLog sqlUndoLog)
    {
        List<SQLUndoLog> list = getSQLUndoLogs() == null ? new ArrayList<>() : getSQLUndoLogs();
        list.add(sqlUndoLog);
        SQL_UNDO_LOG.set(list);
    }

    public static SQLUndoLog getLastSQLUndoLog()
    {
        return getSQLUndoLogs().get(getSQLUndoLogs().size() - 1);
    }

    public static List<SQLUndoLog> getSQLUndoLogs()
    {
        return SQL_UNDO_LOG.get();
    }

    public static void clear()
    {
        CURRENT_BT.remove();
        XID.remove();
        WHETHER_FIRST_EXECUTE.remove();
        SQL_UNDO_LOG.remove();
    }

    /**
     * 全局事务初始化
     */
    public static void GTInit(String xid)
    {
        setXid(xid);
        BTInit(xid);
    }

    /**
     * 分支事务初始化
     */
    public static void BTInit(String xid)
    {
        setBT(BT.builder()
                .xid(xid)
                .status(StatusEnum.START.getCode())
                .build());//绑定BT到当前线程
    }

    /**
     * 普通本地事务初始化
     */
    public static void CTInit(TransactionStatus status)
    {
        setBT(BT.builder().transactionStatus(status).build());
    }
}
