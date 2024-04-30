package cn.distribute.entity.database;

import cn.distribute.entity.database.undoExecutor.AbstractUndoExecutor;
import cn.distribute.entity.database.undoExecutor.UndoDeleteExecutor;
import cn.distribute.entity.database.undoExecutor.UndoInsertExecutor;
import cn.distribute.entity.database.undoExecutor.UndoUpdateExecutor;

/**
 * 2024-04-29 16:20
 * Author: Aurora
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
