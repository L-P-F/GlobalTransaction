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
    protected static final String CHECK_SQL = "SELECT * FROM %s WHERE %s";
    protected static final String NON_CONDITION_CHECK_SQL = "SELECT * FROM %s";

    public abstract SQLUndoLog buildSQLUndoLog(String sql, Connection connection, String name) throws SQLException;

    public abstract ResultSet getResultSet(String sql, Connection connection) throws SQLException;
}
