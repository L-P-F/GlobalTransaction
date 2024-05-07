package cn.aurora.entity.database.undoExecutor;

import cn.aurora.entity.database.SQLUndoLog;
import cn.aurora.entity.database.entity.Field;
import cn.aurora.entity.database.entity.KeyType;
import cn.aurora.entity.database.entity.Row;
import cn.aurora.entity.database.entity.TableData;
import cn.aurora.until.CommonUtil;
import org.apache.ibatis.mapping.SqlCommandType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 2024-04-29 16:25
 * <p>Author: Aurora-LPF</p>
 */
public class UndoInsertExecutor extends AbstractUndoExecutor
{
    private static final SqlCommandType INSERT = SqlCommandType.INSERT;
    private static final String DELETE_SQL = "DELETE FROM %s WHERE %s";

    @Override
    public SQLUndoLog buildSQLUndoLog(String sql, Connection connection, String tableName) throws SQLException
    {
        return SQLUndoLog.buildSQLUndoLog(getResultSet(sql, connection), INSERT, getPrimaryKey(tableName,connection));
    }

    @Override
    public void bindAfterImage(String sql, SQLUndoLog sqlUndoLog, Connection connection) throws SQLException
    {
        List<Object> beforePrimaryKeyValues = new ArrayList<>();
        List<Object> afterPrimaryKeyValues = new ArrayList<>();
        TableData afterImage = TableData.buildTableData(getResultSet(sql, connection), sqlUndoLog.getCurrTablePrimaryKey());


        for (Row row : sqlUndoLog.getBeforeImage().getRows())
        {
            List<Field> list = row.getFields().stream().filter(field -> field.getKeyType().equals(KeyType.PRIMARY_KEY)).toList();
            beforePrimaryKeyValues.add(list.get(0).getValue());
        }
        sqlUndoLog.setBeforeImage(null);
        for (Row row : afterImage.getRows())
        {
            List<Field> list = row.getFields().stream().filter(field -> field.getKeyType().equals(KeyType.PRIMARY_KEY)).toList();
            afterPrimaryKeyValues.add(list.get(0).getValue());
        }
        afterPrimaryKeyValues.removeAll(beforePrimaryKeyValues);
        List<Row> rows = afterImage.getRows().stream()
                .filter(row -> row.getFields().stream()
                        .filter(field -> field.getKeyType().equals(KeyType.PRIMARY_KEY))
                        .map(Field::getValue)
                        .anyMatch(afterPrimaryKeyValues::contains))
                .collect(Collectors.toList());


        afterImage.setRows(rows);
        afterImage.setPrimaryKeyValues(afterPrimaryKeyValues);
        sqlUndoLog.setAfterImage(afterImage);
    }

    @Override
    protected ResultSet getCurrResultSet(SQLUndoLog sqlUndoLog, Connection connection) throws SQLException
    {
        List<Object> primaryKeyValues = sqlUndoLog.getAfterImage().getPrimaryKeyValues();
        StringBuilder condition = new StringBuilder(sqlUndoLog.getCurrTablePrimaryKey() + " in(");
        for (int i = 0; i < primaryKeyValues.size(); i++)
        {
            if (i != 0)
                condition.append(",").append(primaryKeyValues.get(i));
            else
                condition.append(primaryKeyValues.get(i));
        }
        condition.append(")");
        String checkSql = String.format(CHECK_SQL, sqlUndoLog.getAfterImage().getTableName(), condition);
        return connection.prepareStatement(checkSql).executeQuery();
    }

    @Override
    protected String buildUndoSQL(TableData afterImage)
    {
        List<Field> primaryKeys = afterImage.getRows().get(0).primaryKeys();
        StringBuilder condition = new StringBuilder();
        for (int i = 0; i < primaryKeys.size(); i++)
        {
            if (i != 0) condition.append(" and ");
            condition.append(primaryKeys.get(i).getFieldName()).append(" = ?");
        }
        return String.format(DELETE_SQL, afterImage.getTableName(), condition);
    }

    private ResultSet getResultSet(String sql, Connection connection) throws SQLException
    {
        // 使用正则表达式提取INSERT语句中的表名、插入的列和值
        Pattern pattern = Pattern.compile("^INSERT\\s+INTO\\s+(\\w+)\\s*\\(([^)]+)\\)\\s+VALUES\\s*(\\(([^)]+)\\)(?:\\s*,\\s*\\(([^)]+)\\))*)$");
        Matcher matcher = pattern.matcher(sql);
        if (matcher.find())
        {
            String tableName = matcher.group(1);
            String columns = matcher.group(2);
            String values = matcher.group(3);


            String[] split = columns.split(",");
            for (int i = 0; i < split.length; i++)
            {
                if (split[i].trim().equals(getPrimaryKey(tableName, connection)))
                {
                    tryLock(tableName, i, values, connection);
                    break;
                }
            }

            String checkSql = String.format(NON_CONDITION_CHECK_SQL, tableName);
            return connection.prepareStatement(checkSql).executeQuery();
        } else
            throw new SQLException("Invalid INSERT statement: " + sql);
    }

    private void tryLock(String tableName, int index, String values, Connection connection) throws SQLException
    {
        Pattern pattern = Pattern.compile("\\([^)]+\\)");
        Matcher matcher = pattern.matcher(values);
        List<Object> primaryKeyValues = new ArrayList<>();
        while (matcher.find())
        {
            primaryKeyValues.add(matcher.group()
                    .substring(1, matcher.group().length() - 1)
                    .split(",")[index]
                    .trim());
        }
        CommonUtil.tryLock(tableName, primaryKeyValues, connection);
    }
}
