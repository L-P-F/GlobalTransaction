package cn.distribute.entity.database.entity;

import lombok.Data;

/**
 * 2024-04-29 21:00
 * Author: Aurora
 */
@Data
public class Field
{
    private String fieldName;
    private int type;
    private Object value;
    private KeyType keyType = KeyType.NUll;
}
