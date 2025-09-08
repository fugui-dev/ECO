package com.example.eco.filter;

import com.example.eco.util.JwtTokenUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AuthInterceptor implements HandlerInterceptor {


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String authorization = request.getHeader("Authorization");

        if (!StringUtils.hasLength(authorization) || !authorization.startsWith("Bearer ")) {
            throw new RuntimeException("Token 不能为空");
        }

        Boolean tokenExpired = Boolean.FALSE;

        try {
            tokenExpired = JwtTokenUtil.isTokenExpired(authorization);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Token 错误");
        }
        
        if (tokenExpired) {
            throw new RuntimeException("Token 错误或已过期");
        }

        return true;
    }


}
