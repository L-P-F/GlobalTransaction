package cn.distribute.context;

import cn.distribute.entity.BT;
import cn.distribute.entity.database.SQLUndoLog;
import cn.distribute.enums.StatusEnum;

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
    private static final ThreadLocal<AtomicBoolean> WHETHER_FIRST_SEND = new ThreadLocal<>();
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

    public static void setWhetherFirstSend(AtomicBoolean firstSend)
    {
        WHETHER_FIRST_SEND.set(firstSend);
    }

    public static AtomicBoolean getWhetherFirstSend()
    {
        return WHETHER_FIRST_SEND.get();
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
        WHETHER_FIRST_SEND.remove();
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
    public static void CTInit()
    {
        setBT(BT.builder().build());
    }
}
