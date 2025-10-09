package com.example.eco.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class ApiLogAspect {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 定义切点：拦截所有Controller层的public方法
     */
    @Pointcut("execution(public * com.example.eco.api..*.*(..))")
    public void apiLog() {
    }

    /**
     * 前置通知：记录请求信息
     */
    @Before("apiLog()")
    public void doBefore(JoinPoint joinPoint) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                log.info("=== API请求开始 ===");
                log.info("请求URL: {}", request.getRequestURL().toString());
                log.info("请求方法: {}", request.getMethod());
                log.info("请求IP: {}", getClientIpAddress(request));
                log.info("类方法: {}.{}", joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName());
                log.info("请求参数: {}", Arrays.toString(joinPoint.getArgs()));
            }
        } catch (Exception e) {
            log.error("记录API请求日志异常", e);
        }
    }

    /**
     * 后置通知：记录响应信息
     */
    @AfterReturning(pointcut = "apiLog()", returning = "result")
    public void doAfterReturning(JoinPoint joinPoint, Object result) {
        try {
            log.info("方法返回值: {}", objectMapper.writeValueAsString(result));
            log.info("=== API请求结束 ===");
        } catch (Exception e) {
            log.error("记录API响应日志异常", e);
        }
    }

    /**
     * 异常通知：记录异常信息
     */
    @AfterThrowing(pointcut = "apiLog()", throwing = "exception")
    public void doAfterThrowing(JoinPoint joinPoint, Throwable exception) {
        try {
            log.error("=== API请求异常 ===");
            log.error("异常方法: {}.{}", joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName());
            log.error("异常信息: {}", exception.getMessage(), exception);
            log.error("=== API请求异常结束 ===");
        } catch (Exception e) {
            log.error("记录API异常日志异常", e);
        }
    }

    /**
     * 环绕通知：记录方法执行时间
     */
    @Around("apiLog()")
    public Object doAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = proceedingJoinPoint.proceed();
        long endTime = System.currentTimeMillis();
        
        log.info("方法执行时间: {}ms", endTime - startTime);
        return result;
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0];
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
