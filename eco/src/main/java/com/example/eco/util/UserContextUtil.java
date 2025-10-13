package com.example.eco.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 用户上下文工具类
 * 用于获取当前登录用户的信息
 */
@Slf4j
@Component
public class UserContextUtil {
    
    private static JwtUtil jwtUtil;
    
    @Resource
    public void setJwtUtil(JwtUtil jwtUtil) {
        UserContextUtil.jwtUtil = jwtUtil;
    }
    
    /**
     * 获取当前登录用户的钱包地址
     * 直接从JWT token中解析
     * @return 钱包地址，如果未登录返回null
     */
    public static String getCurrentWalletAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                log.warn("无法获取ServletRequestAttributes，可能不在Web请求上下文中");
                return null;
            }
            
            HttpServletRequest request = attributes.getRequest();
            String authorization = request.getHeader("Authorization");
            
            if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
                log.debug("未找到有效的Authorization头");
                return null;
            }
            
            // 提取token
            String token = authorization.substring(7); // 移除"Bearer "前缀
            
            // 从JWT token中解析钱包地址
            if (jwtUtil != null) {
                String address = jwtUtil.getAddressFromToken(token);
                if (StringUtils.hasText(address)) {
                    log.debug("从JWT token解析钱包地址成功: {}", address);
                    return address;
                }
            } else {
                log.warn("JwtUtil未注入，无法解析JWT token");
            }
            
            log.debug("未找到当前用户钱包地址");
            return null;
            
        } catch (Exception e) {
            log.error("获取当前用户钱包地址失败", e);
            return null;
        }
    }
    
    /**
     * 检查当前用户是否已登录
     * @return 是否已登录
     */
    public static boolean isLoggedIn() {
        return getCurrentWalletAddress() != null;
    }
}
