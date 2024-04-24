package cn.distribute.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.transaction.TransactionStatus;

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
    private String bid; //分支事务id
    private String xid; //全局事务id
    private int status; //分支事务状态
    private int executeOrder; //分支事务执行顺序

    @JSONField(serialize = false) //客户端控制事务回滚或提交时使用的字段，不需要序列化
    private TransactionStatus transactionStatus; //事务状态
}
