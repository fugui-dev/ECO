package com.example.eco.config;

import com.example.eco.interceptor.JwtInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                // 只拦截用户端接口
                .addPathPatterns("/v1/user/**")
                // 排除认证相关接口
                .excludePathPatterns(
                        "/v1/user/auth/**",  // 认证接口不需要JWT验证
                        "/v1/user/auth/nonce/**",  // 生成nonce接口
                        "/v1/user/auth/verify",    // 验证签名接口
                        "/v1/user/auth/validate"   // 验证token接口
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
