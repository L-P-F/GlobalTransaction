package cn.distribute.rpc;

import cn.distribute.entity.BT;
import cn.distribute.entity.database.SQLUndoLog;
import cn.distribute.entity.database.UndoExecutorFactory;
import cn.distribute.entity.database.undoExecutor.AbstractUndoExecutor;
import cn.distribute.enums.ReqPathEnum;
import cn.distribute.enums.StatusEnum;
import jakarta.websocket.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 2024-04-22 20:09
 * <p>Author: Aurora-LPF</p>
 */

@Slf4j
@ClientEndpoint
public class SocketClient
{
    private Session session;

    private String latestMessage;

    private final AtomicBoolean flag = new AtomicBoolean(false);

    @OnOpen
    public void onOpen(Session session)
    {
        this.session = session;
    }

    @OnMessage
    public void onMessage(String message)
    {
        log.debug("服务器发送指示: " + (StatusEnum.TRUE.getMsg().equals(message) ? "提交" : "回滚") + ", 时间: {}", LocalDateTime.now());
        latestMessage = message;
        flag.set(true);
        if (session != null && session.isOpen())
        {
            try
            {
                session.close();
            } catch (IOException e)
            {
                log.error("与GT服务器断开连接时出现异常: {},请通知开发人员", e.getMessage());
            }
        }
    }

    @OnClose
    public void onClose()
    {
        log.debug("关闭连接,当前分支事务结束");
    }

    private void judgeMessage(BT bt, DataSource dataSource, List<SQLUndoLog> sqlUndoLogs, StatusEnum exceptionEnum)
    {
        boolean commitOrRollback = StatusEnum.TRUE.getMsg().equals(latestMessage);
        log.warn("{} 号分支事务收到服务器指令: " + (commitOrRollback ? "【提交】" : "【回滚】"), bt.getExecuteOrder());
        if (!commitOrRollback)
        {
            int undoIndex = sqlUndoLogs.size() - 1;
            switch (exceptionEnum)
            {
                case NONE_EXCEPTION, SERVER_EXCEPTION -> undoIndex = sqlUndoLogs.size() - 1;
                case SQL_EXCEPTION -> undoIndex--;
            }

            try (Connection connection = dataSource.getConnection())
            {
                for (int i = undoIndex; i >= 0; i--)
                {
                    SQLUndoLog sqlUndoLog = sqlUndoLogs.get(i);
                    if(sqlUndoLog.getBeforeImage() == null && sqlUndoLog.getAfterImage() == null)
                        continue;
                    AbstractUndoExecutor undoExecutor = UndoExecutorFactory.getUndoExecutor(sqlUndoLog.getSqlCommandType());
                    undoExecutor.rollback(sqlUndoLog, connection);
                }
            } catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }
        log.warn("{} 号分支事务" + (commitOrRollback ? "【提交】" : "【回滚】") + "成功", bt.getExecuteOrder());
        log.warn("全局事务" + (commitOrRollback ? "【提交】" : "【回滚】") + "成功,与服务器断开连接");
    }

    private void connectToServer(BT bt, String executeStatus, DataSource dataSource, List<SQLUndoLog> sqlUndoLogs, StatusEnum exceptionEnum)
    {
        try
        {
            URI uri = new URI(ReqPathEnum.WEB_SOCKET_CONNECT.getUrl() + bt.getXid() + ("/") + (bt.getExecuteOrder()));
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();

            container.connectToServer(this, uri);
            container.setDefaultMaxSessionIdleTimeout(6000L);

            log.warn("{} 号分支事务成功对接服务器,发送执行状态: {}", bt.getExecuteOrder(), executeStatus);
            session.getBasicRemote().sendText(executeStatus);

            while (true)
                if (flag.compareAndSet(true, false))
                    break;
            judgeMessage(bt, dataSource, sqlUndoLogs, exceptionEnum);
        } catch (URISyntaxException | DeploymentException | IOException e)
        {
            log.error("连接GT服务器出现异常: {},已控制所有事务【回滚】,请检查是否已经成功启动【GT-server】", e.getMessage());
            latestMessage = StatusEnum.FALSE.getMsg();
            judgeMessage(bt, dataSource, sqlUndoLogs, exceptionEnum);
        }
    }

    @Async(value = "asyncTaskExecutor")
    public void BTTryToConnect(BT bt, String executeStatus, DataSource dataSource, List<SQLUndoLog> sqlUndoLogs, StatusEnum exceptionEnum)
    {
        connectToServer(bt, executeStatus, dataSource, sqlUndoLogs, exceptionEnum);
    }

    public void GTTryToConnect(BT bt, String executeStatus, DataSource dataSource, List<SQLUndoLog> sqlUndoLogs, StatusEnum exceptionEnum)
    {
        connectToServer(bt, executeStatus, dataSource, sqlUndoLogs, exceptionEnum);
    }
}