package cn.aurora.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 2024-05-03 9:44
 * <p>Author: Aurora-LPF</p>
 */
@Data
@ConfigurationProperties("gt.server")
public class GTConfigurationProperties
{
    private String serverAddr;
    private String undoTableName;
}
