package cn.aurora.entity.database.entity;

import lombok.Data;

/**
 * 2024-04-29 21:00
 * <p>Author: Aurora-LPF</p>
 * <p>目标表的字段</p>
 */
@Data
public class Field
{
    private String fieldName;

    private int type;

    private Object value;

    private KeyType keyType = KeyType.NUll;
}
