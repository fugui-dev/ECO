package com.example.eco.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 */
@Slf4j
@Component
public class JwtUtil {
    
    @Value("${jwt.secret:eco-web3-auth-secret-key-2024}")
    private String secret;
    
    @Value("${jwt.expiration:3600}")
    private Long expiration; // 过期时间，单位：秒，默认1小时
    
    /**
     * 生成JWT token
     * @param address 钱包地址
     * @return JWT token
     */
    public String generateToken(String address) {
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("address", address);
            claims.put("type", "web3");
            
            return createToken(claims, address);
        } catch (Exception e) {
            log.error("生成JWT token失败: address={}", address, e);
            throw new RuntimeException("生成JWT token失败", e);
        }
    }
    
    /**
     * 创建token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration * 1000);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }
    
    /**
     * 从token中获取钱包地址
     * @param token JWT token
     * @return 钱包地址
     */
    public String getAddressFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.get("address", String.class);
        } catch (Exception e) {
            log.error("从token获取地址失败: token={}", token, e);
            return null;
        }
    }
    
    /**
     * 验证token是否有效
     * @param token JWT token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return !isTokenExpired(claims);
        } catch (Exception e) {
            log.error("验证token失败: token={}", token, e);
            return false;
        }
    }
    
    /**
     * 从token中获取Claims
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * 检查token是否过期
     */
    private boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration.before(new Date());
    }
    
    /**
     * 获取token过期时间
     * @param token JWT token
     * @return 过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration();
        } catch (Exception e) {
            log.error("获取token过期时间失败: token={}", token, e);
            return null;
        }
    }
}
