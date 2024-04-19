package cn.distribute.entity;

import lombok.Builder;
import lombok.Data;
import org.apache.ibatis.mapping.SqlCommandType;

import java.util.HashMap;

/*2024-04-18 16:57
 * Author: Aurora
 * 分支事务
 */
@Data
@Builder
public class BT
{
    private String xid; //全局事务id
    private String bid; //分支事务id
    private int executeOrder; //分支事务执行顺序
    private int status; //分支事务状态
    private HashMap<SqlCommandType,String> sql; //当前分支事务执行过的所有sql语句的undo_log
}
