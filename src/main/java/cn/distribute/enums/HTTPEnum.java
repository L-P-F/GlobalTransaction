package cn.distribute.enums;

import lombok.Getter;

/*2024-04-19 21:35
 * Author: Aurora
 */
@Getter
public enum HTTPEnum
{
    SAVE("http://localhost:8573/save"),
    GET("http://localhost:8573/get/");

    private final String url;

    HTTPEnum(String url)
    {
        this.url = url;
    }
}
