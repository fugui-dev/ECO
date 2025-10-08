package com.example.eco.core.service.impl;

import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.dto.ComputingPowerDTO;
import com.example.eco.core.service.ComputingPowerCacheService;
import com.example.eco.core.service.ComputingPowerService;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * 增强版算力计算服务 - 集成缓存功能
 */
@Slf4j
@Service("computingPowerServiceV2")
public class ComputingPowerServiceImplV2 implements ComputingPowerService {

    @Resource
    private ComputingPowerServiceImpl computingPowerService;
    @Resource
    private ComputingPowerCacheService computingPowerCacheService;

    @Override
    public SingleResponse<BigDecimal> calculateUserTotalPower(String walletAddress, String dayTime) {
        // 先尝试从缓存获取
        SingleResponse<ComputingPowerDTO> cachedResponse = computingPowerCacheService.getCachedComputingPower(walletAddress, dayTime);
        if (cachedResponse.isSuccess()) {
            return SingleResponse.of(cachedResponse.getData().getTotalPower());
        }

        // 缓存未命中，计算并缓存
        SingleResponse<BigDecimal> result = computingPowerService.calculateUserTotalPower(walletAddress, dayTime);
        if (result.isSuccess()) {
            // 获取完整算力信息并缓存
            SingleResponse<ComputingPowerDTO> fullInfo = computingPowerService.getComputingPowerInfo(walletAddress, dayTime);
            if (fullInfo.isSuccess()) {
                computingPowerCacheService.cacheComputingPower(walletAddress, dayTime, fullInfo.getData());
            }
        }
        
        return result;
    }

    @Override
    public SingleResponse<BigDecimal> calculateUserSelfPower(String walletAddress, String dayTime) {
        // 先尝试从缓存获取
        SingleResponse<ComputingPowerDTO> cachedResponse = computingPowerCacheService.getCachedComputingPower(walletAddress, dayTime);
        if (cachedResponse.isSuccess()) {
            return SingleResponse.of(cachedResponse.getData().getSelfPower());
        }

        // 缓存未命中，计算并缓存
        SingleResponse<BigDecimal> result = computingPowerService.calculateUserSelfPower(walletAddress, dayTime);
        if (result.isSuccess()) {
            // 获取完整算力信息并缓存
            SingleResponse<ComputingPowerDTO> fullInfo = computingPowerService.getComputingPowerInfo(walletAddress, dayTime);
            if (fullInfo.isSuccess()) {
                computingPowerCacheService.cacheComputingPower(walletAddress, dayTime, fullInfo.getData());
            }
        }
        
        return result;
    }

    @Override
    public SingleResponse<BigDecimal> calculateUserRecommendPower(String walletAddress, String dayTime) {
        // 先尝试从缓存获取
        SingleResponse<ComputingPowerDTO> cachedResponse = computingPowerCacheService.getCachedComputingPower(walletAddress, dayTime);
        if (cachedResponse.isSuccess()) {
            return SingleResponse.of(cachedResponse.getData().getRecommendPower());
        }

        // 缓存未命中，计算并缓存
        SingleResponse<BigDecimal> result = computingPowerService.calculateUserRecommendPower(walletAddress, dayTime);
        if (result.isSuccess()) {
            // 获取完整算力信息并缓存
            SingleResponse<ComputingPowerDTO> fullInfo = computingPowerService.getComputingPowerInfo(walletAddress, dayTime);
            if (fullInfo.isSuccess()) {
                computingPowerCacheService.cacheComputingPower(walletAddress, dayTime, fullInfo.getData());
            }
        }
        
        return result;
    }

    @Override
    public SingleResponse<BigDecimal> calculateUserDirectRecommendPower(String walletAddress, String dayTime) {
        // 先尝试从缓存获取
        SingleResponse<ComputingPowerDTO> cachedResponse = computingPowerCacheService.getCachedComputingPower(walletAddress, dayTime);
        if (cachedResponse.isSuccess()) {
            return SingleResponse.of(cachedResponse.getData().getDirectRecommendPower());
        }

        // 缓存未命中，计算并缓存
        SingleResponse<BigDecimal> result = computingPowerService.calculateUserDirectRecommendPower(walletAddress, dayTime);
        if (result.isSuccess()) {
            // 获取完整算力信息并缓存
            SingleResponse<ComputingPowerDTO> fullInfo = computingPowerService.getComputingPowerInfo(walletAddress, dayTime);
            if (fullInfo.isSuccess()) {
                computingPowerCacheService.cacheComputingPower(walletAddress, dayTime, fullInfo.getData());
            }
        }
        
        return result;
    }

    @Override
    public SingleResponse<BigDecimal> calculateUserMinPower(String walletAddress, String dayTime) {
        // 先尝试从缓存获取
        SingleResponse<ComputingPowerDTO> cachedResponse = computingPowerCacheService.getCachedComputingPower(walletAddress, dayTime);
        if (cachedResponse.isSuccess()) {
            return SingleResponse.of(cachedResponse.getData().getMinPower());
        }

        // 缓存未命中，计算并缓存
        SingleResponse<BigDecimal> result = computingPowerService.calculateUserMinPower(walletAddress, dayTime);
        if (result.isSuccess()) {
            // 获取完整算力信息并缓存
            SingleResponse<ComputingPowerDTO> fullInfo = computingPowerService.getComputingPowerInfo(walletAddress, dayTime);
            if (fullInfo.isSuccess()) {
                computingPowerCacheService.cacheComputingPower(walletAddress, dayTime, fullInfo.getData());
            }
        }
        
        return result;
    }

    @Override
    public SingleResponse<BigDecimal> calculateUserNewPower(String walletAddress, String dayTime) {
        // 先尝试从缓存获取
        SingleResponse<ComputingPowerDTO> cachedResponse = computingPowerCacheService.getCachedComputingPower(walletAddress, dayTime);
        if (cachedResponse.isSuccess()) {
            return SingleResponse.of(cachedResponse.getData().getNewPower());
        }

        // 缓存未命中，计算并缓存
        SingleResponse<BigDecimal> result = computingPowerService.calculateUserNewPower(walletAddress, dayTime);
        if (result.isSuccess()) {
            // 获取完整算力信息并缓存
            SingleResponse<ComputingPowerDTO> fullInfo = computingPowerService.getComputingPowerInfo(walletAddress, dayTime);
            if (fullInfo.isSuccess()) {
                computingPowerCacheService.cacheComputingPower(walletAddress, dayTime, fullInfo.getData());
            }
        }
        
        return result;
    }

    @Override
    public SingleResponse<ComputingPowerDTO> getComputingPowerInfo(String walletAddress, String dayTime) {
        // 先尝试从缓存获取
        SingleResponse<ComputingPowerDTO> cachedResponse = computingPowerCacheService.getCachedComputingPower(walletAddress, dayTime);
        if (cachedResponse.isSuccess()) {
            return cachedResponse;
        }

        // 缓存未命中，计算并缓存
        SingleResponse<ComputingPowerDTO> result = computingPowerService.getComputingPowerInfo(walletAddress, dayTime);
        if (result.isSuccess()) {
            computingPowerCacheService.cacheComputingPower(walletAddress, dayTime, result.getData());
        }
        
        return result;
    }

    @Override
    public SingleResponse<BigDecimal> calculateTotalPower(String dayTime) {
        // 先尝试从缓存获取总算力
        SingleResponse<BigDecimal> cachedResponse = computingPowerCacheService.getCachedTotalPower(dayTime);
        if (cachedResponse.isSuccess()) {
            return cachedResponse;
        }

        // 缓存未命中，计算并缓存
        SingleResponse<BigDecimal> result = computingPowerService.calculateTotalPower(dayTime);
        if (result.isSuccess()) {
            computingPowerCacheService.cacheTotalPower(dayTime, result.getData());
        }
        
        return result;
    }

    /**
     * 清除用户算力缓存（当用户矿机状态变化时调用）
     * @param walletAddress 钱包地址
     */
    public void invalidateUserCache(String walletAddress) {
        computingPowerCacheService.clearUserCache(walletAddress);
        log.info("用户{}算力缓存已清除", walletAddress);
    }

    /**
     * 清除指定日期的算力缓存（当需要重新计算某日算力时调用）
     * @param dayTime 日期
     */
    public void invalidateDayCache(String dayTime) {
        computingPowerCacheService.clearDayCache(dayTime);
        log.info("日期{}算力缓存已清除", dayTime);
    }

    @Override
    public SingleResponse<BigDecimal> calculateUserRecommendPower(String walletAddress, String dayTime, Map<Integer, BigDecimal> levelRateMap, Boolean isLevel) {
        // 推荐算力计算不需要阶梯计算，直接调用原有方法
        return calculateUserRecommendPower(walletAddress, dayTime);
    }

    @Override
    public SingleResponse<BigDecimal> calculateUserMinPower(String walletAddress, String dayTime, Map<Integer, BigDecimal> levelRateMap, Boolean isLevel) {
        // 先尝试从缓存获取
        SingleResponse<ComputingPowerDTO> cachedResponse = computingPowerCacheService.getCachedComputingPower(walletAddress, dayTime);
        if (cachedResponse.isSuccess()) {
            return SingleResponse.of(cachedResponse.getData().getMinPower());
        }

        // 缓存未命中，计算并缓存
        SingleResponse<BigDecimal> result = computingPowerService.calculateUserMinPower(walletAddress, dayTime, levelRateMap, isLevel);
        if (result.isSuccess()) {
            // 获取完整算力信息并缓存
            SingleResponse<ComputingPowerDTO> fullInfo = computingPowerService.getComputingPowerInfo(walletAddress, dayTime);
            if (fullInfo.isSuccess()) {
                computingPowerCacheService.cacheComputingPower(walletAddress, dayTime, fullInfo.getData());
            }
        }
        
        return result;
    }

    @Override
    public SingleResponse<BigDecimal> calculateUserNewPower(String walletAddress, String dayTime, Map<Integer, BigDecimal> levelRateMap, Boolean isLevel) {
        // 先尝试从缓存获取
        SingleResponse<ComputingPowerDTO> cachedResponse = computingPowerCacheService.getCachedComputingPower(walletAddress, dayTime);
        if (cachedResponse.isSuccess()) {
            return SingleResponse.of(cachedResponse.getData().getNewPower());
        }

        // 缓存未命中，计算并缓存
        SingleResponse<BigDecimal> result = computingPowerService.calculateUserNewPower(walletAddress, dayTime, levelRateMap, isLevel);
        if (result.isSuccess()) {
            // 获取完整算力信息并缓存
            SingleResponse<ComputingPowerDTO> fullInfo = computingPowerService.getComputingPowerInfo(walletAddress, dayTime);
            if (fullInfo.isSuccess()) {
                computingPowerCacheService.cacheComputingPower(walletAddress, dayTime, fullInfo.getData());
            }
        }
        
        return result;
    }
}