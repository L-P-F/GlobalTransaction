package cn.distribute.entity.database.undoExecutor;

import cn.distribute.entity.database.SQLUndoLog;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 2024-04-29 16:22
 * Author: Aurora
 */
public abstract class AbstractUndoExecutor
{
    protected static final Map<String, Map<String, String>> table = new HashMap<>();
    protected static final String CHECK_SQL = "SELECT * FROM %s WHERE %s FOR UPDATE";
    protected static final String NON_CONDITION_CHECK_SQL = "SELECT * FROM %s FOR UPDATE";

    public abstract SQLUndoLog buildSQLUndoLog(String sql, Connection connection, String name) throws SQLException;

    public abstract void bindAfterImage(String sql, SQLUndoLog sqlUndoLog, Connection connection) throws SQLException;

    public void rollback(SQLUndoLog sqlUndoLog, Connection connection) throws SQLException
    {
        switch (sqlUndoLog.getSqlCommandType())
        {
            case INSERT:
            case UPDATE:

        }
    }

    protected String getPrimaryKey(String tableName, Connection connection) throws SQLException
    {
        // 获取当前表的主键
        ResultSet primaryKeys = connection.getMetaData().getPrimaryKeys(null, null, tableName);
        String primaryKey = null;
        if (primaryKeys.next())
            primaryKey = primaryKeys.getString("COLUMN_NAME");
        return primaryKey;
    }
}
