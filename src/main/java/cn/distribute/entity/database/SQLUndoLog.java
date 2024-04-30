package cn.distribute.entity.database;

import cn.distribute.entity.database.entity.Field;
import cn.distribute.entity.database.entity.KeyType;
import cn.distribute.entity.database.entity.Row;
import cn.distribute.entity.database.entity.TableData;
import lombok.Data;
import org.apache.ibatis.mapping.SqlCommandType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 2024-04-29 17:07
 * Author: Aurora
 */
@Data
public class SQLUndoLog
{
    private String currTablePrimaryKey;

    private SqlCommandType sqlCommandType;

    private TableData beforeImage;

    private TableData afterImage;

    private SQLUndoLog()
    {
    }

    public static SQLUndoLog buildSQLUndoLog(ResultSet resultSet, SqlCommandType sqlCommandType, String primaryKey) throws SQLException
    {
        SQLUndoLog sqlUndoLog = new SQLUndoLog();

        sqlUndoLog.setSqlCommandType(sqlCommandType);
        sqlUndoLog.setCurrTablePrimaryKey(primaryKey);
        sqlUndoLog.setBeforeImage(TableData.buildTableData(resultSet, primaryKey));

        return sqlUndoLog;
    }

    public void bindAfterImage(ResultSet resultSet) throws SQLException
    {
        switch (this.sqlCommandType)
        {
            case INSERT ->
            {
                List<Object> primaryKeyValues = new ArrayList<>();
                TableData afterImage = TableData.buildTableData(resultSet, this.currTablePrimaryKey);
                for (Row row : this.beforeImage.getRows())
                {
                    List<Field> list = row.getFields().stream().filter(field -> field.getKeyType().equals(KeyType.PRIMARY_KEY)).toList();
                    primaryKeyValues.add(list.get(0).getValue());
                }
//                this.beforeImage = null;
                System.err.println("primaryKeyValues = " + primaryKeyValues);//todo
                for (Row row : afterImage.getRows())
                {
                    List<Field> list = row.getFields().stream().filter(field -> field.getKeyType().equals(KeyType.PRIMARY_KEY)).toList();
                    primaryKeyValues.remove(list.get(0).getValue());
                }
                System.err.println("primaryKeyValues = " + primaryKeyValues);//todo
                List<Row> rows = afterImage.getRows().stream()
                        .filter(row -> row.getFields().stream()
                                .filter(field -> field.getKeyType().equals(KeyType.PRIMARY_KEY))
                                .map(Field::getValue)
                                .noneMatch(primaryKeyValues::contains))
                        .collect(Collectors.toList());
                afterImage.setRows(rows);
                this.setAfterImage(afterImage);
            }
            case DELETE -> this.setAfterImage(null);
            case UPDATE -> this.setAfterImage(TableData.buildTableData(resultSet, this.currTablePrimaryKey));
        }
    }
}
