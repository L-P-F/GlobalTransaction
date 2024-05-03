package cn.distribute.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/*2024-05-03 9:44
 * Author: Aurora
 */
@Data
@ConfigurationProperties("gt.server")
public class GTConfigurationProperties
{
    private String serverAddr;
    private String undoTableName;
}
