package cn.distribute.enums;

import lombok.Getter;

/*2024-04-19 21:35
 * Author: Aurora
 */
@Getter
public enum ReqPathEnum
{
    HTTP_SAVE("http://localhost:8573/save/"),
    WEB_SOCKET_COMMIT("ws://localhost:8573/ws/");

    private final String url;

    ReqPathEnum(String url)
    {
        this.url = url;
    }
}
