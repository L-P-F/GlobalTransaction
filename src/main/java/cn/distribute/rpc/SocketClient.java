package cn.distribute.rpc;

import cn.distribute.entity.TransactionResource;
import cn.distribute.enums.ReqPathEnum;
import cn.distribute.enums.StatusEnum;
import jakarta.websocket.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.TransactionStatus;
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
@Scope("prototype")//每次创建新的socketClient防止并发出现数据安全问题
@ClientEndpoint
public class SocketClient
{
    private Session session;

    private String latestMessage;

    private final AtomicBoolean flag = new AtomicBoolean(false);

    @OnOpen
    public void onOpen(Session session)
    {
        log.warn("事务对接服务器成功,会话对象为{}", session);
        this.session = session;
    }

    @OnMessage
    public void onMessage(String message)
    {
        log.warn("时间: {},服务器发送指示{}", LocalDateTime.now(), message);
        latestMessage = message;
        flag.set(true);
        close();
    }

    @OnClose
    public void onClose()
    {
        log.warn("关闭连接,当前分支事务结束");
    }

    /**
     * 关闭websocket连接
     */
    private void close()
    {
        if (session != null && session.isOpen())
            try
            {
                session.close();
            } catch (IOException e)
            {
                log.error("与GT服务器断开连接时出现异常: {}", e.getMessage());
            }
    }

    private void BTJudgeMessage(TransactionTemplate transactionTemplate, TransactionStatus status,
                               TransactionResource transactionResource)
    {
        log.warn("收到服务器指令{},执行操作...", latestMessage);
        transactionResource.autoWiredTransactionResource();
        if (StatusEnum.TRUE.getMsg().equals(latestMessage))
            Objects.requireNonNull(transactionTemplate.getTransactionManager()).commit(status);
        else
            Objects.requireNonNull(transactionTemplate.getTransactionManager()).rollback(status);
        transactionResource.removeTransactionResource();
    }

    private void GTJudgeMessage(TransactionTemplate transactionTemplate, TransactionStatus status)
    {
        log.warn("收到服务器指令{},执行操作...", latestMessage);
        if (StatusEnum.TRUE.getMsg().equals(latestMessage))
            Objects.requireNonNull(transactionTemplate.getTransactionManager()).commit(status);
        else
            Objects.requireNonNull(transactionTemplate.getTransactionManager()).rollback(status);
    }


    private void connectToServer(String executeStatus, String xid, TransactionTemplate transactionTemplate,
                                TransactionStatus status, TransactionResource transactionResource)
    {
        try
        {
            URI uri = new URI(ReqPathEnum.WEB_SOCKET_COMMIT.getUrl() + xid);
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, uri);
            container.setDefaultMaxSessionIdleTimeout(6000L);
            session.getBasicRemote().sendText(executeStatus);
            while (true)
                if(flag.compareAndSet(true,false))
                    break;
            if (transactionResource == null)
                GTJudgeMessage(transactionTemplate, status);
            else
                BTJudgeMessage(transactionTemplate, status, transactionResource);
        } catch (URISyntaxException | DeploymentException | IOException  e)
        {
            log.error("连接GT服务器出现异常", e);
        }
    }

    @Async
    public void BTTryToConnect(String executeStatus, String xid, TransactionTemplate transactionTemplate, TransactionStatus status, TransactionResource transactionResource)
    {
        connectToServer(executeStatus, xid, transactionTemplate, status, transactionResource);
    }

    public void GTTryToConnect(String executeStatus, String xid, TransactionTemplate transactionTemplate, TransactionStatus status)
    {
        connectToServer(executeStatus, xid, transactionTemplate, status, null);
    }
}