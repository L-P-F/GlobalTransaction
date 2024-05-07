package cn.aurora.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 2024-04-17 14:23
 * <p>Author: Aurora-LPF</p>
 * <p>全局事务入口注解,标记全局事务开启</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GlobalTransaction
{
}
