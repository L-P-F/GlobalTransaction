package cn.distribute.entity.database.undoExecutor;

import cn.distribute.entity.database.SQLUndoLog;
import cn.distribute.entity.database.entity.Field;
import cn.distribute.entity.database.entity.Row;
import cn.distribute.entity.database.entity.TableData;
import cn.distribute.until.CommonUtil;
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
public class UndoUpdateExecutor extends AbstractUndoExecutor
{
    private static final SqlCommandType UPDATE = SqlCommandType.UPDATE;
    private static final String UPDATE_SQL = "UPDATE %s SET %s WHERE %s";

    @Override
    public SQLUndoLog buildSQLUndoLog(String sql, Connection connection, String tableName) throws SQLException
    {
        SQLUndoLog sqlUndoLog = SQLUndoLog.buildSQLUndoLog(getResultSet(sql, connection), UPDATE, getPrimaryKey(tableName, connection));
        CommonUtil.tryLock(tableName,sqlUndoLog.getBeforeImage().getPrimaryKeyValues(), connection);
        return sqlUndoLog;
    }

    @Override
    public void bindAfterImage(String sql, SQLUndoLog sqlUndoLog, Connection connection) throws SQLException
    {
        if(sqlUndoLog.getBeforeImage().getRows().size() == 0)
            sqlUndoLog.setBeforeImage(null);
        else sqlUndoLog.setAfterImage(TableData.buildTableData(getAfterResultSet(sqlUndoLog, connection), sqlUndoLog.getCurrTablePrimaryKey()));
    }

    @Override
    protected ResultSet getCurrResultSet(SQLUndoLog sqlUndoLog, Connection connection) throws SQLException
    {
        return getResultSet(sqlUndoLog, connection, sqlUndoLog.getAfterImage().getPrimaryKeyValues());
    }

    @Override
    protected String buildUndoSQL(TableData beforeImage)
    {
        Row row = beforeImage.getRows().get(0);
        List<Field> nonPrimaryKeysFields = row.nonPrimaryKeys();
        String sets = nonPrimaryKeysFields.stream()
                .map(field -> field.getFieldName() + " = ?")
                .collect(Collectors.joining(", "));

        List<Field> primaryKeys = row.primaryKeys();
        StringBuilder condition = new StringBuilder();
        for (int i = 0; i < primaryKeys.size(); i++)
        {
            if (i != 0) condition.append(" and ");
            condition.append(primaryKeys.get(i).getFieldName()).append(" = ?");
        }

        return String.format(UPDATE_SQL, beforeImage.getTableName(), sets, condition);
    }

    private ResultSet getAfterResultSet(SQLUndoLog sqlUndoLog, Connection connection) throws SQLException
    {
        return getResultSet(sqlUndoLog, connection, sqlUndoLog.getBeforeImage().getPrimaryKeyValues());
    }

    private ResultSet getResultSet(SQLUndoLog sqlUndoLog, Connection connection, List<Object> primaryKeyValues) throws SQLException
    {
        StringBuilder condition = new StringBuilder(sqlUndoLog.getCurrTablePrimaryKey() + " in(");
        for (int i = 0; i < primaryKeyValues.size(); i++)
        {
            if (i != 0)
                condition.append(",").append(primaryKeyValues.get(i));
            else
                condition.append(primaryKeyValues.get(i));
        }
        condition.append(")");
        String checkSql = String.format(CHECK_SQL, sqlUndoLog.getBeforeImage().getTableName(), condition);
        return connection.prepareStatement(checkSql).executeQuery();
    }

    private ResultSet getResultSet(String sql, Connection connection) throws SQLException
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
