package cn.distribute.entity.database;

import cn.distribute.entity.database.entity.TableData;
import lombok.Data;
import org.apache.ibatis.mapping.SqlCommandType;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 2024-04-29 17:07
 * Author: Aurora
 */
@Data
public class SQLUndoLog
{
    /**
     * 当前表的主键名
     */
    private String currTablePrimaryKey;

    /**
     * 当前sql操作类型
     */
    private SqlCommandType sqlCommandType;

    /**
     * 前置镜像
     */
    private TableData beforeImage;

    /**
     * 后置镜像
     */
    private TableData afterImage;

    private SQLUndoLog()
    {
    }

    public static SQLUndoLog buildSQLUndoLog(ResultSet resultSet, SqlCommandType sqlCommandType, String primaryKey) throws SQLException
    {
        SQLUndoLog sqlUndoLog = new SQLUndoLog();

        sqlUndoLog.setSqlCommandType(sqlCommandType);
        sqlUndoLog.setCurrTablePrimaryKey(primaryKey);
        sqlUndoLog.setBeforeImage(TableData.buildTableData(resultSet, primaryKey,sqlCommandType));

        return sqlUndoLog;
    }
}
