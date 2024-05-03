package cn.distribute.until;

import java.io.Serializable;
import java.sql.Array;
import java.sql.SQLException;
import java.util.Map;

/**
 * 2024-05-01 14:07
 * <p>Author: Aurora-LPF</p>
 * <p>借鉴seata中的SerialArray,如有冒犯请告知</p>
 */
public class SerialArray extends javax.sql.rowset.serial.SerialArray implements Array, Serializable
{
    private Object[] elements;
    private int len;

    public SerialArray(Array array, Map<String, Class<?>> map) throws SQLException
    {
        super(array, map);
    }

    public SerialArray(Array array) throws SQLException
    {
        super(array);
    }

    public void setElems(Object[] elements)
    {
        this.elements = elements;
        this.len = elements != null ? elements.length : 0;
    }
    public Object[] getElements()
    {
        return elements;
    }
}
