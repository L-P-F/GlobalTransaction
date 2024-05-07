package cn.aurora.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 2024-04-18 11:01
 * <p>Author: Aurora-LPF</p>
 * <p>分支事务注解,添加在serviceImpl方法上</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BranchTransaction
{

}
