package cn.aurora.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2024-04-18 10:59
 * <p>Author: Aurora-LPF</p>
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T>
{

    /**
     * 业务上的成功或失败
     */
    private boolean success;

    /**
     * 返回信息
     */
    private String message;

    /**
     * 返回泛型数据，自定义类型，携带数据
     */
    private T content;
}
