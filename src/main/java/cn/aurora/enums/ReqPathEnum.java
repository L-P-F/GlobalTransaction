package cn.aurora.enums;

import lombok.Getter;

/**
 * 2024-04-19 21:35
 * <p>Author: Aurora-LPF</p>
 */
@Getter
public enum ReqPathEnum
{
    HTTP_SAVE("http://","/save/"),
    WEB_SOCKET_CONNECT("ws://","/ws/");

    private final String urlPrefix;
    private final String urlSuffix;

    ReqPathEnum(String urlPrefix,String urlSuffix)
    {
        this.urlPrefix = urlPrefix;
        this.urlSuffix = urlSuffix;
    }
}
