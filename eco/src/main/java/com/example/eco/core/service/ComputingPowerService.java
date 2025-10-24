package com.example.eco.core.service;

import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.dto.ComputingPowerDTO;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 算力计算服务接口
 */
public interface ComputingPowerService {

    /**
     * 计算用户总算力（包含自身和下级）
     * @param walletAddress 钱包地址
     * @param dayTime 日期
     * @return 总算力
     */
    SingleResponse<BigDecimal> calculateUserTotalPower(String walletAddress, String dayTime);

    /**
     * 计算用户自身算力（不包含下级）
     * @param walletAddress 钱包地址
     * @param dayTime 日期
     * @return 自身算力
     */
    SingleResponse<BigDecimal> calculateUserSelfPower(String walletAddress, String dayTime);

    /**
     * 计算用户推荐算力（下级算力总和）
     * @param walletAddress 钱包地址
     * @param dayTime 日期
     * @return 推荐算力
     */
    SingleResponse<BigDecimal> calculateUserRecommendPower(String walletAddress, String dayTime);

    /**
     * 计算用户推荐算力（下级算力总和）- 支持阶梯计算
     * @param walletAddress 钱包地址
     * @param dayTime 日期
     * @param levelRateMap 层级费率映射
     * @param isLevel 是否按层级计算
     * @return 推荐算力
     */
    SingleResponse<BigDecimal> calculateUserRecommendPower(String walletAddress, String dayTime, Map<Integer, BigDecimal> levelRateMap, Boolean isLevel);

    /**
     * 计算用户直推算力（直接下级算力总和）
     * @param walletAddress 钱包地址
     * @param dayTime 日期
     * @return 直推算力
     */
    SingleResponse<BigDecimal> calculateUserDirectRecommendPower(String walletAddress, String dayTime);

    /**
     * 计算用户小区算力（除最大算力外的直推算力总和）
     * @param walletAddress 钱包地址
     * @param dayTime 日期
     * @return 小区算力
     */
    SingleResponse<BigDecimal> calculateUserMinPower(String walletAddress, String dayTime);

    /**
     * 计算用户小区算力（除最大算力外的直推算力总和）- 支持阶梯计算
     * @param walletAddress 钱包地址
     * @param dayTime 日期
     * @param levelRateMap 层级费率映射
     * @param isLevel 是否按层级计算
     * @return 小区算力
     */
    SingleResponse<BigDecimal> calculateUserMinPower(String walletAddress, String dayTime, Map<Integer, BigDecimal> levelRateMap, Boolean isLevel);

    /**
     * 计算用户新增算力（当日新增的算力）
     * @param walletAddress 钱包地址
     * @param dayTime 日期
     * @return 新增算力
     */
    SingleResponse<BigDecimal> calculateUserNewPower(String walletAddress, String dayTime);

    /**
     * 计算用户新增算力（当日新增的算力）- 支持阶梯计算
     * @param walletAddress 钱包地址
     * @param dayTime 日期
     * @param levelRateMap 层级费率映射
     * @param isLevel 是否按层级计算
     * @return 新增算力
     */
    SingleResponse<BigDecimal> calculateUserNewPower(String walletAddress, String dayTime, Map<Integer, BigDecimal> levelRateMap, Boolean isLevel);

    /**
     * 获取完整的算力信息
     * @param walletAddress 钱包地址
     * @param dayTime 日期
     * @return 算力信息
     */
    SingleResponse<ComputingPowerDTO> getComputingPowerInfo(String walletAddress, String dayTime);


    /**
     * 获取完整的算力信息
     * @param walletAddress 钱包地址
     * @param dayTime 日期
     * @return 算力信息
     */
    SingleResponse<ComputingPowerDTO> getAllComputingPowerInfo(String walletAddress, String dayTime,Boolean isLevel);

    /**
     * 计算所有用户的总算力
     * @param dayTime 日期
     * @return 总算力
     */
    SingleResponse<BigDecimal> calculateTotalPower(String dayTime);
}