package cn.distribute.enums;

import lombok.Getter;

/*2024-04-19 10:55
 * Author: Aurora
 */
@Getter
public enum StatusEnum
{
    UNKNOWN(-1,"未知"),
    START(0,"开始"),
    COMMIT(1,"提交"),
    ROLLBACK(2,"回滚");

    private final int code;
    private final String msg;

    StatusEnum(int code, String msg)
    {
        this.code = code;
        this.msg = msg;
    }
}
