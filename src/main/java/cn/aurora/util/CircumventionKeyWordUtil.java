package cn.aurora.util;

import cn.aurora.enums.MySQLKeyWordEnum;

/**
 * 2024-05-01 17:13
 * <p>Author: Aurora-LPF</p>
 * <p>Description: 对于与MySQL数据库关键字冲突的字段进行加反引号处理</p>
 */
public class CircumventionKeyWordUtil
{
    public static String Convert(String columnName)
    {
        if(MySQLKeyWordEnum.isKeyWord(columnName))
            columnName = "`" + columnName + "`";
        return columnName;
    }
}
