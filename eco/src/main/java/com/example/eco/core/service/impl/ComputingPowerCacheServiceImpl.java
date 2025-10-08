package com.example.eco.core.service.impl;

import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.dto.ComputingPowerDTO;
import com.example.eco.core.service.ComputingPowerCacheService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ComputingPowerCacheServiceImpl implements ComputingPowerCacheService {

    @Resource
    private RedissonClient redissonClient;

    private static final String CACHE_PREFIX = "computing_power:";
    private static final String TOTAL_POWER_PREFIX = "total_power:";
    private static final int CACHE_EXPIRE_HOURS = 24; // 缓存24小时

    @Override
    public SingleResponse<ComputingPowerDTO> getCachedComputingPower(String walletAddress, String dayTime) {
        try {
            String cacheKey = CACHE_PREFIX + walletAddress + ":" + dayTime;
            RBucket<ComputingPowerDTO> bucket = redissonClient.getBucket(cacheKey);
            
            ComputingPowerDTO cached = bucket.get();
            if (cached != null) {
                log.debug("从缓存获取用户{}算力信息", walletAddress);
                return SingleResponse.of(cached);
            }
            
            return SingleResponse.buildFailure("缓存中无数据");
        } catch (Exception e) {
            log.error("获取缓存算力信息失败: {}", e.getMessage(), e);
            return SingleResponse.buildFailure("获取缓存失败: " + e.getMessage());
        }
    }

    @Override
    public void cacheComputingPower(String walletAddress, String dayTime, ComputingPowerDTO computingPower) {
        try {
            String cacheKey = CACHE_PREFIX + walletAddress + ":" + dayTime;
            RBucket<ComputingPowerDTO> bucket = redissonClient.getBucket(cacheKey);
            bucket.set(computingPower, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            
            log.debug("缓存用户{}算力信息", walletAddress);
        } catch (Exception e) {
            log.error("缓存算力信息失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public void clearUserCache(String walletAddress) {
        try {
            String pattern = CACHE_PREFIX + walletAddress + ":*";
            redissonClient.getKeys().deleteByPattern(pattern);
            
            log.info("清除用户{}的算力缓存", walletAddress);
        } catch (Exception e) {
            log.error("清除用户缓存失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public void clearDayCache(String dayTime) {
        try {
            String pattern = CACHE_PREFIX + "*:" + dayTime;
            redissonClient.getKeys().deleteByPattern(pattern);
            
            // 同时清除总算力缓存
            String totalPowerKey = TOTAL_POWER_PREFIX + dayTime;
            redissonClient.getBucket(totalPowerKey).delete();
            
            log.info("清除日期{}的算力缓存", dayTime);
        } catch (Exception e) {
            log.error("清除日期缓存失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public void clearAllCache() {
        try {
            String pattern = CACHE_PREFIX + "*";
            redissonClient.getKeys().deleteByPattern(pattern);
            
            String totalPattern = TOTAL_POWER_PREFIX + "*";
            redissonClient.getKeys().deleteByPattern(totalPattern);
            
            log.info("清除所有算力缓存");
        } catch (Exception e) {
            log.error("清除所有缓存失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public SingleResponse<BigDecimal> getCachedTotalPower(String dayTime) {
        try {
            String cacheKey = TOTAL_POWER_PREFIX + dayTime;
            RBucket<BigDecimal> bucket = redissonClient.getBucket(cacheKey);
            
            BigDecimal cached = bucket.get();
            if (cached != null) {
                log.debug("从缓存获取总算力: {}", cached);
                return SingleResponse.of(cached);
            }
            
            return SingleResponse.buildFailure("缓存中无总算力数据");
        } catch (Exception e) {
            log.error("获取缓存总算力失败: {}", e.getMessage(), e);
            return SingleResponse.buildFailure("获取缓存失败: " + e.getMessage());
        }
    }

    @Override
    public void cacheTotalPower(String dayTime, BigDecimal totalPower) {
        try {
            String cacheKey = TOTAL_POWER_PREFIX + dayTime;
            RBucket<BigDecimal> bucket = redissonClient.getBucket(cacheKey);
            bucket.set(totalPower, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            
            log.debug("缓存总算力: {}", totalPower);
        } catch (Exception e) {
            log.error("缓存总算力失败: {}", e.getMessage(), e);
        }
    }
}