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
public class UndoUpdateExecutor extends AbstractUndoExecutor
{
    private static final SqlCommandType UPDATE = SqlCommandType.UPDATE;
    private static final String UPDATE_SQL = "UPDATE %s SET %s WHERE %s ";

    @Override
    public SQLUndoLog buildSQLUndoLog(String sql, Connection connection, String tableName) throws SQLException
    {
        ResultSet primaryKeys = connection.getMetaData().getPrimaryKeys(null, null, tableName);
        String primaryKey = null;
        if (primaryKeys.next())
            primaryKey = primaryKeys.getString("COLUMN_NAME");
        return SQLUndoLog.buildSQLUndoLog(getResultSet(sql, connection), UPDATE, primaryKey);
    }

    @Override
    public ResultSet getResultSet(String sql, Connection connection) throws SQLException
    {
        // 使用正则表达式提取UPDATE语句中的表名、更新的列和更新条件
        Pattern pattern = Pattern.compile("^UPDATE\\s+(\\w+)\\s+SET\\s+(.+?)\\s*(?:WHERE\\s+(.+))?$");
        Matcher matcher = pattern.matcher(sql);
        if (matcher.find())
        {
            String tableName = matcher.group(1);
            String changes = matcher.group(2);
            String condition = matcher.group(3);

            String checkSql = (condition != null) ? String.format(CHECK_SQL, tableName, condition) : String.format(NON_CONDITION_CHECK_SQL, tableName);
            return connection.prepareStatement(checkSql).executeQuery();
        } else
            throw new SQLException("Invalid UPDATE SQL: " + sql);
    }
}
