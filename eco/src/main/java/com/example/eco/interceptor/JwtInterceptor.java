package com.example.eco.interceptor;

import com.example.eco.annotation.NoJwtAuth;
import com.example.eco.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT拦截器 - 验证请求中的JWT token
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {
    
    @Resource
    private JwtUtil jwtUtil;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 处理预检请求
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }
        
        String requestURI = request.getRequestURI();
        log.debug("JWT拦截器: 处理请求 URI={}", requestURI);
        
        // 检查是否有跳过JWT验证的注解
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            
            // 检查方法上的注解
            if (handlerMethod.hasMethodAnnotation(NoJwtAuth.class)) {
                log.debug("JWT拦截器: 方法跳过JWT验证, method={}, URI={}", handlerMethod.getMethod().getName(), requestURI);
                return true;
            }
            
            // 检查类上的注解
            if (handlerMethod.getBeanType().isAnnotationPresent(NoJwtAuth.class)) {
                log.debug("JWT拦截器: 类跳过JWT验证, class={}, URI={}", handlerMethod.getBeanType().getSimpleName(), requestURI);
                return true;
            }
        }
        
        // 获取Authorization头
        String authorization = request.getHeader("Authorization");
        
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            log.warn("JWT拦截器: 缺少有效的Authorization头, URI={}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"code\":401,\"errMessage\":\"缺少有效的Authorization头\"}");
            return false;
        }
        
        // 提取token
        String token = authorization.substring(7); // 移除"Bearer "前缀
        
        try {
            // 验证token
            if (!jwtUtil.validateToken(token)) {
                log.warn("JWT拦截器: token无效或已过期, URI={}", request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"code\":401,\"errMessage\":\"token无效或已过期\"}");
                return false;
            }
            
            // 获取钱包地址并设置到请求属性中，供后续使用
            String address = jwtUtil.getAddressFromToken(token);
            if (StringUtils.hasText(address)) {
                request.setAttribute("walletAddress", address);
                log.debug("JWT拦截器: 验证成功, address={}, URI={}", address, request.getRequestURI());
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("JWT拦截器: 验证token时发生异常, URI={}", request.getRequestURI(), e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"code\":401,\"errMessage\":\"token验证失败\"}");
            return false;
        }
    }
}
