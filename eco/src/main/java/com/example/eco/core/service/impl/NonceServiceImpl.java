package com.example.eco.core.service.impl;

import com.example.eco.core.service.NonceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * Nonce服务实现类
 */
@Slf4j
@Service
public class NonceServiceImpl implements NonceService {
    
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    
    private static final String NONCE_PREFIX = "nonce:";
    private static final int NONCE_EXPIRE_MINUTES = 5; // nonce过期时间5分钟
    private static final int NONCE_LENGTH = 32; // nonce长度32字节
    
    @Override
    public String generateAndSaveNonce(String address) {
        try {
            // 生成32字节的随机nonce
            SecureRandom secureRandom = new SecureRandom();
            byte[] nonceBytes = new byte[NONCE_LENGTH];
            secureRandom.nextBytes(nonceBytes);
            
            // 转换为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : nonceBytes) {
                sb.append(String.format("%02x", b));
            }
            String nonce = sb.toString();
            
            // 存储到Redis，设置过期时间
            String key = NONCE_PREFIX + address.toLowerCase();
            stringRedisTemplate.opsForValue().set(key, nonce, NONCE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            
            log.info("生成nonce成功: address={}, nonce={}", address, nonce);
            return nonce;
            
        } catch (Exception e) {
            log.error("生成nonce失败: address={}", address, e);
            throw new RuntimeException("生成nonce失败", e);
        }
    }
    
    @Override
    public String getAndDeleteNonce(String address) {
        try {
            String key = NONCE_PREFIX + address.toLowerCase();

            String nonce = stringRedisTemplate.opsForValue().get(key);
            
            if (nonce != null) {
                // 获取后立即删除
                stringRedisTemplate.delete(key);
                log.info("获取并删除nonce成功: address={}, nonce={}", address, nonce);
                return nonce;
            } else {
                log.warn("nonce不存在或已过期: address={}", address);
                return null;
            }
            
        } catch (Exception e) {
            log.error("获取nonce失败: address={}", address, e);
            throw new RuntimeException("获取nonce失败", e);
        }
    }
    
    @Override
    public boolean existsNonce(String address) {
        try {
            String key = NONCE_PREFIX + address.toLowerCase();
            Boolean exists = stringRedisTemplate.hasKey(key);
            return exists != null && exists;
            
        } catch (Exception e) {
            log.error("检查nonce是否存在失败: address={}", address, e);
            return false;
        }
    }
}
