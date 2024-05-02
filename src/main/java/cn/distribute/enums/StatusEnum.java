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
    FALSE(0, "false"),

    NONE_EXCEPTION(0, "无异常"),
    SQL_EXCEPTION(1, "SQL异常"),
    SERVER_EXCEPTION(2, "服务器异常");

    private final Integer code;
    private final String msg;

    StatusEnum(Integer code, String msg)
    {
        this.code = code;
        this.msg = msg;
    }
}
