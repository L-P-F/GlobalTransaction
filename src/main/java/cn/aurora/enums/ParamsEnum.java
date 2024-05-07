package cn.aurora.enums;

import lombok.Getter;

/**
 * 2024-04-18 17:24
 * <p>Author: Aurora-LPF</p>
 */
@Getter
public enum ParamsEnum
{
    XID("xid", "全局事务ID");
    private final String value;
    private final String content;

    ParamsEnum(String value, String content)
    {
        this.value = value;
        this.content = content;
    }
}
