package cn.distribute.until;

import cn.distribute.enums.MySQLKeyWord;

/*2024-05-01 17:13
 * Author: Aurora
 */
public class CircumventionKeyWord
{
    public static String Convert(String columnName)
    {
        if(MySQLKeyWord.isKeyWord(columnName))
            columnName = "`" + columnName + "`";
        return columnName;
    }
}
