package com.example.eco.core.service;

import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.dto.ComputingPowerDTO;

import java.math.BigDecimal;

/**
 * 算力缓存服务接口
 */
public interface ComputingPowerCacheService {

    /**
     * 获取缓存的算力信息
     * @param walletAddress 钱包地址
     * @param dayTime 日期
     * @return 算力信息
     */
    SingleResponse<ComputingPowerDTO> getCachedComputingPower(String walletAddress, String dayTime);

    /**
     * 缓存算力信息
     * @param walletAddress 钱包地址
     * @param dayTime 日期
     * @param computingPower 算力信息
     */
    void cacheComputingPower(String walletAddress, String dayTime, ComputingPowerDTO computingPower);

    /**
     * 清除用户算力缓存
     * @param walletAddress 钱包地址
     */
    void clearUserCache(String walletAddress);

    /**
     * 清除指定日期的算力缓存
     * @param dayTime 日期
     */
    void clearDayCache(String dayTime);

    /**
     * 清除所有算力缓存
     */
    void clearAllCache();

    /**
     * 获取缓存的总算力
     * @param dayTime 日期
     * @return 总算力
     */
    SingleResponse<BigDecimal> getCachedTotalPower(String dayTime);

    /**
     * 缓存总算力
     * @param dayTime 日期
     * @param totalPower 总算力
     */
    void cacheTotalPower(String dayTime, BigDecimal totalPower);
}