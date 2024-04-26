package cn.distribute.rpc;

import cn.distribute.entity.BT;
import cn.distribute.entity.TransactionResource;
import cn.distribute.enums.ReqPathEnum;
import cn.distribute.enums.StatusEnum;
import jakarta.websocket.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
/*2024-04-22 20:09
 * Author: Aurora
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
    public void onClose() {log.debug("关闭连接,当前分支事务结束");}

    private void judgeMessage(BT bt, TransactionTemplate transactionTemplate, TransactionResource transactionResource)
    {
        boolean commitOrRollback = StatusEnum.TRUE.getMsg().equals(latestMessage);
        log.warn("{} 号分支事务收到服务器指令: " + (commitOrRollback ? "【提交】" : "【回滚】"), bt.getExecuteOrder());
        if (transactionResource != null)
            transactionResource.autoWiredTransactionResource();

        if (commitOrRollback)
            Objects.requireNonNull(transactionTemplate.getTransactionManager()).commit(bt.getTransactionStatus());
        else
            Objects.requireNonNull(transactionTemplate.getTransactionManager()).rollback(bt.getTransactionStatus());

        if (transactionResource != null)
        {
            transactionResource.removeTransactionResource();
            log.warn("{} 号分支事务" + (commitOrRollback ? "【提交】" : "【回滚】") + "成功", bt.getExecuteOrder());
        } else
            log.warn("全局事务" + (commitOrRollback ? "【提交】" : "【回滚】") + "成功,与服务器断开连接");
    }

    private void connectToServer(BT bt, String executeStatus, TransactionTemplate transactionTemplate, TransactionResource transactionResource)
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
            judgeMessage(bt, transactionTemplate, transactionResource);
        } catch (URISyntaxException | DeploymentException | IOException e)
        {
            log.error("连接GT服务器出现异常: {},已控制所有事务【回滚】,请检查是否已经成功启动【GT-server】", e.getMessage());
            latestMessage = StatusEnum.FALSE.getMsg();
            judgeMessage(bt, transactionTemplate, transactionResource);
        }
    }

    @Async(value = "asyncTaskExecutor")
    public void BTTryToConnect(BT bt, String executeStatus, TransactionTemplate transactionTemplate, TransactionResource transactionResource)
    {
        connectToServer(bt, executeStatus, transactionTemplate, transactionResource);
    }

    public void GTTryToConnect(BT bt, String executeStatus, TransactionTemplate transactionTemplate)
    {
        connectToServer(bt, executeStatus, transactionTemplate, null);
    }
}