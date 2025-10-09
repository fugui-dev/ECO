package com.example.eco.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API日志注解
 * 用于标记需要记录日志的接口方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiLog {
    
    /**
     * 日志描述
     */
    String value() default "";
    
    /**
     * 是否记录请求参数
     */
    boolean logRequest() default true;
    
    /**
     * 是否记录响应结果
     */
    boolean logResponse() default true;
    
    /**
     * 是否记录执行时间
     */
    boolean logTime() default true;
}
