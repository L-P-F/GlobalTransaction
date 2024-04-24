package cn.distribute.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/*2024-04-23 20:21
 * Author: Aurora
 */
@Data
@ConfigurationProperties(prefix = "spring.datasource")
public class DatasourceProperties
{
    private String url;
    private String username;
    private String password;
}
