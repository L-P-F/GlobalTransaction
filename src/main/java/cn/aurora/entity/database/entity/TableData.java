package cn.aurora.entity.database.entity;

import cn.aurora.until.CircumventionKeyWord;
import lombok.Data;

import javax.sql.rowset.serial.*;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 2024-04-29 21:03
 * <p>Author: Aurora-LPF</p>
 * <p>获取的镜像数据</p>
 */
@Data
public class TableData
{
    /**
     * 当前前置镜像的所有记录的主键的值
     */
    private List<Object> primaryKeyValues = new ArrayList<>();

    private String tableName;

    private List<Row> rows = new ArrayList<>();

    private TableData()
    {
    }

    public static TableData buildTableData(ResultSet resultSet, String primaryKey) throws SQLException
    {
        TableData tableData = new TableData();
        ResultSetMetaData metaData = resultSet.getMetaData();
        tableData.setTableName(metaData.getTableName(1)); // todo 获取表名，由于一个结果集中可能由于sql的复杂程度导致数据不来自用一个表，这样获取表名不严谨
        int columnCount = metaData.getColumnCount();

        while (resultSet.next())
        {
            List<Field> fields = new ArrayList<>();
            Row row = new Row();
            for (int i = 1; i <= columnCount; i++)
            {
                Field field = new Field();
                String columnName = CircumventionKeyWord.Convert(metaData.getColumnName(i));


                if (columnName.equals(primaryKey))
                    field.setKeyType(KeyType.PRIMARY_KEY);
                else field.setKeyType(KeyType.NUll);

                field.setFieldName(columnName);

                int valueType = metaData.getColumnType(i);
                field.setType(valueType);

                switch (valueType)
                {
                    case Types.BLOB ->
                    {
                        Blob blob = resultSet.getBlob(i);
                        if (blob != null)
                            field.setValue(new SerialBlob(blob));
                        if (field.getKeyType() == KeyType.PRIMARY_KEY && blob != null)
                            tableData.primaryKeyValues.add(new SerialBlob(blob));
                    }
                    case Types.CLOB ->
                    {
                        Clob clob = resultSet.getClob(i);
                        if (clob != null)
                            field.setValue(new SerialClob(clob));
                        if(field.getKeyType() == KeyType.PRIMARY_KEY && clob != null)
                            tableData.primaryKeyValues.add(new SerialClob(clob));
                    }
                    case Types.NCLOB ->
                    {
                        NClob nClob = resultSet.getNClob(i);
                        if (nClob != null)
                            field.setValue(new SerialClob(nClob));
                        if(field.getKeyType() == KeyType.PRIMARY_KEY && nClob != null)
                            tableData.primaryKeyValues.add(new SerialClob(nClob));
                    }
                    case Types.ARRAY ->
                    {
                        Array array = resultSet.getArray(i);
                        if (array != null)
                            field.setValue(new SerialArray(array));
                        if(field.getKeyType() == KeyType.PRIMARY_KEY && array != null)
                            tableData.primaryKeyValues.add(new SerialArray(array));
                    }
                    case Types.REF ->
                    {
                        Ref ref = resultSet.getRef(i);
                        if (ref != null)
                            field.setValue(new SerialRef(ref));
                        if(field.getKeyType() == KeyType.PRIMARY_KEY && ref != null)
                            tableData.primaryKeyValues.add(new SerialRef(ref));
                    }
                    case Types.DATALINK ->
                    {
                        URL url = resultSet.getURL(i);
                        if (url != null)
                            field.setValue(new SerialDatalink(url));
                        if(field.getKeyType() == KeyType.PRIMARY_KEY && url != null)
                            tableData.primaryKeyValues.add(new SerialDatalink(url));
                    }
                    case Types.JAVA_OBJECT ->
                    {
                        Object object = resultSet.getObject(i);
                        if (object != null)
                            field.setValue(new SerialJavaObject(object));
                        if(field.getKeyType() == KeyType.PRIMARY_KEY && object != null)
                            tableData.primaryKeyValues.add(new SerialJavaObject(object));
                    }
                    default ->
                    {
                        // JDBCType.DISTINCT, JDBCType.STRUCT etc...
                        field.setValue(holdSerialDataType(resultSet.getObject(i)));
                        if(field.getKeyType() == KeyType.PRIMARY_KEY)
                            tableData.primaryKeyValues.add(holdSerialDataType(resultSet.getObject(i)));
                    }
                }

                fields.add(field);
            }
            row.setFields(fields);

            tableData.add(row);
        }

        return tableData;
    }

    private static Object holdSerialDataType(Object data) throws SQLException
    {
        if (null == data)
            return null;

        if (data instanceof Blob blob)
            return new SerialBlob(blob);

        if (data instanceof NClob nClob)
            return new SerialClob(nClob);

        if (data instanceof Clob clob)
            return new SerialClob(clob);

        return data;
    }

    private void add(Row row)
    {
        this.rows.add(row);
    }
}
