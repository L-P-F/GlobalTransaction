package cn.distribute.entity.database;

import cn.distribute.entity.database.undoExecutor.AbstractUndoExecutor;
import org.apache.ibatis.mapping.SqlCommandType;

/**
 * 2024-04-29 16:21
 * <p>Author: Aurora-LPF</p>
 * <p>工厂模式管理DML语句三种类型操作的对象</p>
 */
public class UndoExecutorFactory
{
    public static AbstractUndoExecutor getUndoExecutor(SqlCommandType type)
    {
        UndoExecutorHolder holder = new UndoExecutorHolder();
        AbstractUndoExecutor executor = null;
        switch (type)
        {
            case INSERT -> executor = holder.getInsertExecutor();
            case DELETE -> executor = holder.getDeleteExecutor();
            case UPDATE -> executor = holder.getUpdateExecutor();
        }
        return executor;
    }
}
