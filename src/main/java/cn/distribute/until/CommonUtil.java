package cn.distribute.until;

import cn.distribute.enums.ReqPathEnum;
import cn.distribute.properties.GTConfigurationProperties;
import lombok.Data;

/*2024-05-03 12:10
 * Author: Aurora
 */
@Data
public class CommonUtil
{
    private static GTConfigurationProperties gtConfigurationProperties = null;

    public CommonUtil(GTConfigurationProperties gtConfigurationProperties)
    {
        CommonUtil.gtConfigurationProperties = gtConfigurationProperties;
    }

    public static String buildReqPath(ReqPathEnum reqPathEnum)
    {
        return reqPathEnum.getUrlPrefix() + gtConfigurationProperties.getServerAddr() + reqPathEnum.getUrlSuffix();
    }
}
