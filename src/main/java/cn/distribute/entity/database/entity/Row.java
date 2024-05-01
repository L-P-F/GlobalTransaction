package cn.distribute.entity.database.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 2024-04-29 21:00
 * Author: Aurora
 */
@Data
public class Row
{
    private List<Field> fields;

    /**
     * Primary keys list.
     *
     * @return the Primary keys list
     */
    public List<Field> primaryKeys()
    {
        List<Field> pkFields = new ArrayList<>();
        for (Field field : fields)
            if (KeyType.PRIMARY_KEY == field.getKeyType())
                pkFields.add(field);
        return pkFields;
    }

    /**
     * Non-primary keys list.
     *
     * @return the non-primary list
     */
    public List<Field> nonPrimaryKeys()
    {
        List<Field> nonPkFields = new ArrayList<>();
        for (Field field : fields)
            if (KeyType.PRIMARY_KEY != field.getKeyType())
                nonPkFields.add(field);
        return nonPkFields;
    }
}
