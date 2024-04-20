package cn.distribute.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.mapping.SqlCommandType;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

/*2024-04-18 16:57
 * Author: Aurora
 * 分支事务
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BT
{
    private String bid; //分支事务id ,分支事务执行顺序
    private String xid; //全局事务id
    private int status; //分支事务状态
    private int executeOrder; //分支事务执行顺序
    private LinkedHashSet<String> sqlData; //使用LinkedHashSet保证元素的顺序;当前分支事务执行过的所有sql语句的undo_log
}
