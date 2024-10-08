package cn.aurora.rpc;

import cn.aurora.context.GTContext;
import cn.aurora.entity.BT;
import cn.aurora.entity.database.UndoExecutorFactory;
import cn.aurora.entity.database.entity.SQLUndoLog;
import cn.aurora.entity.database.undoExecutor.AbstractUndoExecutor;
import cn.aurora.enums.ReqPathEnum;
import cn.aurora.enums.StatusEnum;
import cn.aurora.util.CommonUtil;
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

    private void judgeMessage(BT bt, DataSource dataSource, List<SQLUndoLog> sqlUndoLogs)
    {
        boolean commitOrRollback = StatusEnum.TRUE.getMsg().equals(latestMessage);
        log.warn("{} | {} 号分支事务收到服务器指令: " + (commitOrRollback ? "【提交】" : "【回滚】"), bt.getXid(), bt.getExecuteOrder());
        try
        {
            if (!commitOrRollback && sqlUndoLogs != null)
            {
                try (Connection connection = dataSource.getConnection())
                {
                    for (int i = sqlUndoLogs.size() - 1; i >= 0; i--)
                    {
                        SQLUndoLog sqlUndoLog = sqlUndoLogs.get(i);
                        if (!sqlUndoLog.getSqlExecStatus()) //如果状态是false,代表当前sql执行失败,是没有对数据造成修改的,不需要回滚,直接跳过
                            continue;
                        if (sqlUndoLog.getBeforeImage() == null && sqlUndoLog.getAfterImage() == null)
                            continue;
                        AbstractUndoExecutor undoExecutor = UndoExecutorFactory.getUndoExecutor(sqlUndoLog.getSqlCommandType());
                        undoExecutor.rollback(sqlUndoLog, connection);
                    }
                } catch (SQLException e)
                {
                    log.error("回滚事务时出现异常: {},请立即通知开发人员!", e.getMessage());
                    throw new RuntimeException("回滚事务时出现异常,请立即通知开发人员!" + e);
                }
            }
        } finally
        {
            try (Connection connection = dataSource.getConnection())
            {
                CommonUtil.releaseLock(bt.getXid(), connection); //无论提交还是回滚，最后都要释放锁
            } catch (SQLException e)
            {
                log.error("GT释放锁时出现异常: {},继续工作会导致连接池资源耗尽!请立即通知开发人员!", e.getMessage());
                throw new RuntimeException("GT释放锁时出现异常,继续工作会导致连接池资源耗尽!请立即通知开发人员!" + e);
            }
        }
        log.warn("{} | {} 号分支事务" + (commitOrRollback ? "【提交】" : "【回滚】") + "成功", bt.getXid(), bt.getExecuteOrder());
        if (bt.getExecuteOrder() == 1)
            log.warn("全局事务 {}" + (commitOrRollback ? "【提交】" : "【回滚】") + "成功,与服务器断开连接", bt.getXid());
    }

    private void connectToServer(BT bt, String executeStatus, DataSource dataSource, List<SQLUndoLog> sqlUndoLogs)
    {
        try
        {
            URI uri = new URI(CommonUtil.buildReqPath(ReqPathEnum.WEB_SOCKET_CONNECT) + bt.getXid() + ("/") + (bt.getExecuteOrder()));
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();

            container.connectToServer(this, uri);
            container.setDefaultMaxSessionIdleTimeout(6000L);

            log.warn("{} | {} 号分支事务成功对接服务器,发送执行状态: {}", bt.getXid(), bt.getExecuteOrder(), executeStatus);
            session.getBasicRemote().sendText(executeStatus);

            while (true)
                if (flag.compareAndSet(true, false))
                    break;
            judgeMessage(bt, dataSource, sqlUndoLogs);
        } catch (URISyntaxException | DeploymentException | IOException e)
        {
            log.error("隶属于全局事务{} | 【连接GT服务器出现异常】: {},已控制当前分支事务【回滚】,请检查是否已经成功启动【GT-server】", GTContext.getXid(), e.getMessage());
            latestMessage = StatusEnum.FALSE.getMsg();
            judgeMessage(bt, dataSource, sqlUndoLogs);
        }
    }

    @Async(value = "asyncTaskExecutor")
    public void BTTryToConnect(BT bt, String executeStatus, DataSource dataSource, List<SQLUndoLog> sqlUndoLogs)
    {
        connectToServer(bt, executeStatus, dataSource, sqlUndoLogs);
    }

    public void GTTryToConnect(BT bt, String executeStatus, DataSource dataSource, List<SQLUndoLog> sqlUndoLogs)
    {
        connectToServer(bt, executeStatus, dataSource, sqlUndoLogs);
    }
}