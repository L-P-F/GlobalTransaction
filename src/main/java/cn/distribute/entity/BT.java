package cn.distribute.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;

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
    private LinkedHashSet<String> sqlData; //使用LinkedHashSet保证元素的顺序;当前分支事务执行过的所有sql语句;偶数存放操作类型,奇数存放前一个操作类型的具体sql
}
