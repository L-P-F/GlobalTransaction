package cn.aurora.entity.database.undoExecutor;

import cn.aurora.entity.database.SQLUndoLog;
import cn.aurora.entity.database.entity.Field;
import cn.aurora.entity.database.entity.Row;
import cn.aurora.entity.database.entity.TableData;
import org.apache.ibatis.mapping.SqlCommandType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 2024-04-29 16:25
 * <p>Author: Aurora-LPF</p>
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
    public void bindAfterImage(String sql, SQLUndoLog sqlUndoLog, Connection connection)
    {
        if(sqlUndoLog.getBeforeImage().getRows().size() == 0)
            sqlUndoLog.setBeforeImage(null);
        sqlUndoLog.setAfterImage(null);
    }

    /**
     * delete的回滚不需要获取比较后置镜像,因此直接返回null
     */
    @Override
    protected ResultSet getCurrResultSet(SQLUndoLog sqlUndoLog, Connection connection)
    {
        return null;
    }

    @Override
    protected String buildUndoSQL(TableData beforeImage)
    {
        Row row = beforeImage.getRows().get(0);
        List<Field> allFields = row.getFields();
        String insertColumnKey = allFields.stream()
                .map(Field::getFieldName)
                .collect(Collectors.joining(", "));

        String insertColumnValue = allFields.stream()
                .map(field -> "?")
                .collect(Collectors.joining(", "));
        return String.format(INSERT_SQL, beforeImage.getTableName(), insertColumnKey, insertColumnValue);
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
