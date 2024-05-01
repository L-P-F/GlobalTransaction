package cn.distribute.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2024-04-18 16:57
 * <p>Author: Aurora-LPF</p>
 * 分支事务
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BT
{
    private String xid; //全局事务id

    @JSONField(serialize = false)
    private String bid; //分支事务id

    private Integer status; //分支事务状态

    @JSONField(serialize = false)
    private Integer executeOrder; //分支事务执行顺序
}
