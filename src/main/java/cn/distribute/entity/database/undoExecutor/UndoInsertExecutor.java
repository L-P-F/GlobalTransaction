package cn.distribute.entity.database.undoExecutor;

import cn.distribute.entity.database.SQLUndoLog;
import cn.distribute.entity.database.entity.Field;
import cn.distribute.entity.database.entity.KeyType;
import cn.distribute.entity.database.entity.Row;
import cn.distribute.entity.database.entity.TableData;
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
 * Author: Aurora
 */
public class UndoInsertExecutor extends AbstractUndoExecutor
{
    private static final String DELETE_SQL = "DELETE FROM %s WHERE %s ";

    @Override
    public SQLUndoLog buildSQLUndoLog(String sql, Connection connection, String tableName) throws SQLException
    {
        return SQLUndoLog.buildSQLUndoLog(getResultSet(sql, connection), SqlCommandType.INSERT, getPrimaryKey(tableName,connection));
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

    private ResultSet getResultSet(String sql, Connection connection) throws SQLException
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
