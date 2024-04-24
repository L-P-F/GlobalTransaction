package cn.distribute.rpc;

import cn.distribute.enums.ReqPathEnum;
import cn.distribute.enums.StatusEnum;
import jakarta.websocket.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
/*2024-04-22 20:09
 * Author: Aurora
 */

@ClientEndpoint
@Slf4j
@Getter
@Scope("prototype")
public class SocketClient
{
    private Session session;

    private String latestMessage;

    @OnOpen
    public void onOpen(Session session)
    {
        log.warn("事务对接服务器成功,会话对象为{}",session);
        this.session = session;
    }

    @OnMessage
    public void onMessage(String message)
    {
        log.warn("时间: {},服务器发送指示{}",LocalDateTime.now(),message);
        latestMessage = message;
        close();
    }

    @OnClose
    public void onClose()
    {
        log.warn("关闭连接,当前分支事务结束");
    }

    public void send(String message)
    {
        this.session.getAsyncRemote().sendText(message);
    }

    public void close()
    {
        if (session != null && session.isOpen())
            try {
                session.close();
            } catch (IOException e) {
                log.error("与GT服务器断开连接时出现异常: {}", e.getMessage());
            }
    }

    public void judgeMessage(TransactionTemplate transactionTemplate, TransactionStatus status)
    {
        log.warn("收到服务器指令{},执行操作...",latestMessage);
        if(StatusEnum.TRUE.getMsg().equals(latestMessage))
            transactionTemplate.getTransactionManager().commit(status);
        else
            transactionTemplate.getTransactionManager().rollback(status);
    }

    @Async
    public void connectToServer(String executeStatus, String xid, TransactionTemplate transactionTemplate, TransactionStatus status)
    {
        try
        {
            URI uri = new URI(ReqPathEnum.WEB_SOCKET_COMMIT.getUrl()+ xid);
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, uri);
            container.setDefaultMaxSessionIdleTimeout(5000L);
            send(executeStatus);
            Thread.sleep(100);
            judgeMessage(transactionTemplate,status);
        } catch (URISyntaxException | DeploymentException | IOException | InterruptedException e)
        {
            log.error("连接GT服务器出现异常", e);
        }
    }

}