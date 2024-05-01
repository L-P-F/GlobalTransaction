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
public class UndoDeleteExecutor extends AbstractUndoExecutor
{
    private static final String INSERT_SQL = "INSERT INTO %s (%s) VALUES (%s)";

    @Override
    public SQLUndoLog buildSQLUndoLog(String sql, Connection connection, String tableName) throws SQLException
    {
        return SQLUndoLog.buildSQLUndoLog(getResultSet(sql, connection), SqlCommandType.DELETE, getPrimaryKey(tableName, connection));
    }

    @Override
    public void bindAfterImage(String sql,SQLUndoLog sqlUndoLog, Connection connection) throws SQLException
    {
        sqlUndoLog.setAfterImage(null);
    }

    private ResultSet getResultSet(String sql, Connection connection) throws SQLException
    {
        // 使用正则表达式提取DELETE语句中的表名和删除条件
        Pattern pattern = Pattern.compile("^DELETE\\s+FROM\\s+(\\w+)\\s+(?:WHERE\\s+(.+))?$");
        Matcher matcher = pattern.matcher(sql);
        if (matcher.find())
        {
            String tableName = matcher.group(1);
            String condition = matcher.group(2);

            String checkSql = String.format(CHECK_SQL, tableName, condition);
            return connection.prepareStatement(checkSql).executeQuery();
        } else
            throw new SQLException("Invalid DELETE statement");
    }

}
