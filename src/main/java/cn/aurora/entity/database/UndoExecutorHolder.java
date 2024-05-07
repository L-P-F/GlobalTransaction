package cn.aurora.entity.database;

import cn.aurora.entity.database.undoExecutor.AbstractUndoExecutor;
import cn.aurora.entity.database.undoExecutor.UndoDeleteExecutor;
import cn.aurora.entity.database.undoExecutor.UndoInsertExecutor;
import cn.aurora.entity.database.undoExecutor.UndoUpdateExecutor;

/**
 * 2024-04-29 16:20
 * <p>Author: Aurora-LPF</p>
 */
public class UndoExecutorHolder
{
    public AbstractUndoExecutor getInsertExecutor()
    {
        return new UndoInsertExecutor();
    }

    public AbstractUndoExecutor getDeleteExecutor()
    {
        return new UndoDeleteExecutor();
    }

    public AbstractUndoExecutor getUpdateExecutor()
    {
        return new UndoUpdateExecutor();
    }
}
