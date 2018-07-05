package com.binfo.monitor.log.model;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * 自定义注解
 * Created by shi.xiulong
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LogRecord {
    String object() default "";
    String identif() default "";
    String type() default "";
    String module() default "";
    String desc() default "";
    String user() default "";
}
