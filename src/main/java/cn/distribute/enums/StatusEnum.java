package cn.distribute.enums;

import lombok.Getter;

/**
 * 2024-04-19 10:55
 * <p>Author: Aurora-LPF</p>
 */
@Getter
public enum StatusEnum
{
    START(0, "开始"),
    COMMIT(1, "提交"),
    ROLLBACK(2, "回滚"),
    TRUE(1, "true"),
    FALSE(0, "false");

    private final Integer code;
    private final String msg;

    StatusEnum(Integer code, String msg)
    {
        this.code = code;
        this.msg = msg;
    }
}
