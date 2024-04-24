package cn.distribute.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*2024-04-17 14:23
 * Author: Aurora
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
//@Transactional
public @interface GlobalTransaction
{
}
