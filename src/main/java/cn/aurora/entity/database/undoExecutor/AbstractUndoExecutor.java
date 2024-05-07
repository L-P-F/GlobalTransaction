package cn.aurora.entity.database.undoExecutor;

import cn.aurora.entity.database.SQLUndoLog;
import cn.aurora.entity.database.entity.Field;
import cn.aurora.entity.database.entity.KeyType;
import cn.aurora.entity.database.entity.Row;
import cn.aurora.entity.database.entity.TableData;
import cn.aurora.until.SerialArray;
import lombok.extern.slf4j.Slf4j;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import javax.sql.rowset.serial.SerialDatalink;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 2024-04-29 16:22
 * <p>Author: Aurora-LPF</p>
 * <P>回滚执行</P>
 * <p>借鉴seata中对于mysql数据库的回滚执行,如有冒犯请告知</p>
 */
@Slf4j
public abstract class AbstractUndoExecutor
{
    protected static final String CHECK_SQL = "SELECT * FROM %s WHERE %s FOR UPDATE";
    protected static final String NON_CONDITION_CHECK_SQL = "SELECT * FROM %s FOR UPDATE";

    public abstract SQLUndoLog buildSQLUndoLog(String sql, Connection connection, String name) throws SQLException;

    public abstract void bindAfterImage(String sql, SQLUndoLog sqlUndoLog, Connection connection) throws SQLException;

    public void rollback(SQLUndoLog sqlUndoLog, Connection connection) throws SQLException
    {
        switch (sqlUndoLog.getSqlCommandType())
        {
            case INSERT, UPDATE ->
            {
                TableData currImage = TableData.buildTableData(getCurrResultSet(sqlUndoLog, connection), sqlUndoLog.getCurrTablePrimaryKey());
                if (!sqlUndoLog.getAfterImage().equals(currImage))
                    throw new RuntimeException("出现不可控异常,数据库信息被非当前事务篡改,请人工处理.\n" +
                            "回滚操作时的数据本应为" + sqlUndoLog.getAfterImage() + "但是实际为" + currImage);
            }
        }


        String undoSQL = null;
        switch (sqlUndoLog.getSqlCommandType())
        {
            case INSERT -> undoSQL = buildUndoSQL(sqlUndoLog.getAfterImage());
            case UPDATE,DELETE -> undoSQL = buildUndoSQL(sqlUndoLog.getBeforeImage());
        }


        try (PreparedStatement statement = connection.prepareStatement(undoSQL))
        {
            switch (sqlUndoLog.getSqlCommandType())
            {
                case INSERT ->
                {
                    for (Row undoRow : sqlUndoLog.getAfterImage().getRows())
                    {
                        undoPrepare(statement, new ArrayList<>(), undoRow.primaryKeys());
                        statement.executeUpdate();
                    }
                }
                case DELETE ->
                {
                    for (Row undoRow : sqlUndoLog.getBeforeImage().getRows())
                    {
                        undoPrepare(statement, new ArrayList<>(), undoRow.getFields());
                        statement.executeUpdate();
                    }
                }
                case UPDATE ->
                {
                    for (Row undoRow : sqlUndoLog.getBeforeImage().getRows())
                    {
                        List<Field> undoValues = new ArrayList<>();
                        for (Field field : undoRow.getFields())
                            if (field.getKeyType() != KeyType.PRIMARY_KEY)
                                undoValues.add(field);

                        undoPrepare(statement, undoValues, undoRow.primaryKeys());
                        statement.executeUpdate();
                    }
                }
            }
        }
    }

    protected abstract ResultSet getCurrResultSet(SQLUndoLog sqlUndoLog, Connection connection) throws SQLException;

    protected abstract String buildUndoSQL(TableData image);

    protected String getPrimaryKey(String tableName, Connection connection) throws SQLException
    {
        // 获取当前表的主键
        ResultSet primaryKeys = connection.getMetaData().getPrimaryKeys(null, null, tableName);
        String primaryKey = null;
        if (primaryKeys.next())
            primaryKey = primaryKeys.getString("COLUMN_NAME");
        return primaryKey;
    }

    private void undoPrepare(PreparedStatement statement, List<Field> undoValues, List<Field> primaryKeyValues) throws SQLException
    {
        int undoIndex = 0;
        for (Field undoValue : undoValues)
        {
            undoIndex++;
            int type = undoValue.getType();
            Object value = undoValue.getValue();
            if (type == JDBCType.BLOB.getVendorTypeNumber())
            {
                SerialBlob serialBlob = (SerialBlob) value;
                if (serialBlob != null)
                    statement.setBytes(undoIndex, serialBlob.getBytes(1, (int) serialBlob.length()));
                else
                    statement.setObject(undoIndex, null);
            } else if (type == JDBCType.CLOB.getVendorTypeNumber() || type == JDBCType.NCLOB.getVendorTypeNumber())
            {
                SerialClob serialClob = (SerialClob) value;
                if (serialClob != null)
                    statement.setClob(undoIndex, serialClob.getCharacterStream());
                else
                    statement.setObject(undoIndex, null);
            } else if (type == JDBCType.DATALINK.getVendorTypeNumber())
            {
                SerialDatalink dataLink = (SerialDatalink) value;
                if (dataLink != null)
                    statement.setURL(undoIndex, dataLink.getDatalink());
                else
                    statement.setObject(undoIndex, null);
            } else if (type == JDBCType.ARRAY.getVendorTypeNumber())
            {
                SerialArray array = (SerialArray) value;
                if (array != null)
                {
                    Array arrayOf = statement.getConnection().createArrayOf(array.getBaseTypeName(), array.getElements());
                    statement.setArray(undoIndex, arrayOf);
                } else
                    statement.setObject(undoIndex, null);
            } else if (undoValue.getType() == JDBCType.OTHER.getVendorTypeNumber())
            {
                statement.setObject(undoIndex, value);
            } else if (undoValue.getType() == JDBCType.BIT.getVendorTypeNumber())
            {
                statement.setObject(undoIndex, value);
            } else
            {
                // JDBCType.REF, JDBCType.JAVA_OBJECT etc...
                statement.setObject(undoIndex, value, type);
            }
        }
        // PK is always at last.
        // INSERT INTO a (x, y, z, pk1,pk2) VALUES (?, ?, ?, ? ,?)
        // UPDATE a SET x=?, y=?, z=? WHERE pk1 in (?) and pk2 in (?)
        // DELETE FROM a WHERE pk1 in (?) and pk2 in (?)
        for (Field pkField : primaryKeyValues)
        {
            undoIndex++;
            statement.setObject(undoIndex, pkField.getValue(), pkField.getType());
        }
    }
}
