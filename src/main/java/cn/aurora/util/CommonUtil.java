package cn.aurora.util;

import cn.aurora.context.GTContext;
import cn.aurora.enums.ReqPathEnum;
import cn.aurora.properties.GTConfigurationProperties;
import lombok.Data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 2024-05-03 12:10
 * <p>Author: Aurora-LPF</p>
 * <p>工具包,进行数据的加锁和释放锁,动态构建访问服务器的请求路径等</p>
 */
@Data
public class CommonUtil
{
    private static GTConfigurationProperties gtConfigurationProperties;

    public CommonUtil(GTConfigurationProperties gtConfigurationProperties)
    {
        CommonUtil.gtConfigurationProperties = gtConfigurationProperties;
    }

    public static String buildReqPath(ReqPathEnum reqPathEnum)
    {
        return reqPathEnum.getUrlPrefix() + gtConfigurationProperties.getServerAddr() + reqPathEnum.getUrlSuffix();
    }

    public static synchronized void tryLock(String tableName, List<Object> primaryKeyValues, Connection connection) throws SQLException
    {
        StringBuilder select = new StringBuilder("select * from ");
        select.append(gtConfigurationProperties.getUndoTableName())
                .append(" where table_name = '").append(tableName).append("' and id in (");


        StringBuilder insert = new StringBuilder("insert into ");
        insert.append(gtConfigurationProperties.getUndoTableName())
                .append("(xid,table_name,id) values");


        for (int i = 0; i < primaryKeyValues.size(); i++)
        {
            if (i > 0)
            {
                select.append(",");
                insert.append(",");
            }
            select.append("'").append(primaryKeyValues.get(i)).append("'");
            insert.append("('").append(GTContext.getXid()).append("','").append(tableName).append("','").append(primaryKeyValues.get(i)).append("')");
        }
        select.append(")");


        ResultSet resultSet = connection.prepareStatement(select.toString()).executeQuery();
        AtomicBoolean hasNext = new AtomicBoolean(false);
        while (resultSet.next())
        {
            hasNext.set(true);
            if (!GTContext.getXid().equals(resultSet.getString("xid")))
                throw new RuntimeException("target data has been locked by other transaction");
        }
        if (!hasNext.get())
        {
            try (PreparedStatement statement = connection.prepareStatement(insert.toString()))
            {
                statement.executeUpdate();
            }
        }
    }

    public static void releaseLock(String xid, Connection connection) throws SQLException
    {
        String delete = "delete from " + gtConfigurationProperties.getUndoTableName() +
                " where xid = '" + xid + "'";
        try (PreparedStatement statement = connection.prepareStatement(delete))
        {
            statement.executeUpdate();
        }
    }
}
