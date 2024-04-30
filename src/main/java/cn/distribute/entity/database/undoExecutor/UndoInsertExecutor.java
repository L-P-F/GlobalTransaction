package cn.distribute.entity.database.undoExecutor;

import cn.distribute.entity.database.SQLUndoLog;
import org.apache.ibatis.mapping.SqlCommandType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 2024-04-29 16:25
 * Author: Aurora
 */
public class UndoInsertExecutor extends AbstractUndoExecutor
{
    private static final String INSERT_SQL = "DELETE FROM %s WHERE %s ";

    @Override
    public SQLUndoLog buildSQLUndoLog(String sql, Connection connection, String tableName) throws SQLException
    {
        ResultSet primaryKeys = connection.getMetaData().getPrimaryKeys(null, null, tableName);
        String primaryKey = null;
        if(primaryKeys.next())
            primaryKey = primaryKeys.getString("COLUMN_NAME");
        return SQLUndoLog.buildSQLUndoLog(getResultSet(sql, connection), SqlCommandType.INSERT, primaryKey);
    }

    @Override
    public ResultSet getResultSet(String sql, Connection connection) throws SQLException
    {
        // 使用正则表达式提取INSERT语句中的表名、插入的列和值
        Pattern pattern = Pattern.compile("^INSERT\\s+INTO\\s+(\\w+)\\s*\\(([^)]+)\\)\\s+VALUES\\s*\\(([^)]+)\\)$");
        Matcher matcher = pattern.matcher(sql);
        if (matcher.find())
        {
            String tableName = matcher.group(1);
            String columns = matcher.group(2);
            String values = matcher.group(3);

            String checkSql = String.format(NON_CONDITION_CHECK_SQL, tableName);
            return connection.prepareStatement(checkSql).executeQuery();
        } else
            throw new SQLException("Invalid INSERT statement: " + sql);
    }
}
