package com.example.eco.util;

import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.dto.ComputingPowerDTO;
import com.example.eco.core.service.ComputingPowerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 算力计算工具类
 * 提供便捷的静态方法调用
 */
@Slf4j
@Component
public class ComputingPowerUtil {

    private static ComputingPowerService computingPowerService;

    @Resource(name = "computingPowerService")
    private ComputingPowerService computingPowerServiceBean;

    @PostConstruct
    public void init() {
        computingPowerService = computingPowerServiceBean;
    }

    /**
     * 获取当前日期的总算力
     * @return 总算力
     */
    public static BigDecimal getTodayTotalPower() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        SingleResponse<BigDecimal> response = computingPowerService.calculateTotalPower(today);
        return response.isSuccess() ? response.getData() : BigDecimal.ZERO;
    }

    /**
     * 获取指定日期的总算力
     * @param dayTime 日期
     * @return 总算力
     */
    public static BigDecimal getTotalPower(String dayTime) {
        SingleResponse<BigDecimal> response = computingPowerService.calculateTotalPower(dayTime);
        return response.isSuccess() ? response.getData() : BigDecimal.ZERO;
    }

    /**
     * 获取用户总算力
     * @param walletAddress 钱包地址
     * @return 总算力
     */
    public static BigDecimal getUserTotalPower(String walletAddress) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        SingleResponse<BigDecimal> response = computingPowerService.calculateUserTotalPower(walletAddress, today);
        return response.isSuccess() ? response.getData() : BigDecimal.ZERO;
    }

    /**
     * 获取用户总算力（指定日期）
     * @param walletAddress 钱包地址
     * @param dayTime 日期
     * @return 总算力
     */
    public static BigDecimal getUserTotalPower(String walletAddress, String dayTime) {
        SingleResponse<BigDecimal> response = computingPowerService.calculateUserTotalPower(walletAddress, dayTime);
        return response.isSuccess() ? response.getData() : BigDecimal.ZERO;
    }

    /**
     * 获取用户自身算力
     * @param walletAddress 钱包地址
     * @return 自身算力
     */
    public static BigDecimal getUserSelfPower(String walletAddress) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        SingleResponse<BigDecimal> response = computingPowerService.calculateUserSelfPower(walletAddress, today);
        return response.isSuccess() ? response.getData() : BigDecimal.ZERO;
    }

    /**
     * 获取用户推荐算力
     * @param walletAddress 钱包地址
     * @return 推荐算力
     */
    public static BigDecimal getUserRecommendPower(String walletAddress) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        SingleResponse<BigDecimal> response = computingPowerService.calculateUserRecommendPower(walletAddress, today);
        return response.isSuccess() ? response.getData() : BigDecimal.ZERO;
    }

    /**
     * 获取用户直推算力
     * @param walletAddress 钱包地址
     * @return 直推算力
     */
    public static BigDecimal getUserDirectRecommendPower(String walletAddress) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        SingleResponse<BigDecimal> response = computingPowerService.calculateUserDirectRecommendPower(walletAddress, today);
        return response.isSuccess() ? response.getData() : BigDecimal.ZERO;
    }

    /**
     * 获取用户小区算力
     * @param walletAddress 钱包地址
     * @return 小区算力
     */
    public static BigDecimal getUserMinPower(String walletAddress) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        SingleResponse<BigDecimal> response = computingPowerService.calculateUserMinPower(walletAddress, today);
        return response.isSuccess() ? response.getData() : BigDecimal.ZERO;
    }

    /**
     * 获取用户新增算力
     * @param walletAddress 钱包地址
     * @return 新增算力
     */
    public static BigDecimal getUserNewPower(String walletAddress) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        SingleResponse<BigDecimal> response = computingPowerService.calculateUserNewPower(walletAddress, today);
        return response.isSuccess() ? response.getData() : BigDecimal.ZERO;
    }

    /**
     * 获取用户完整算力信息
     * @param walletAddress 钱包地址
     * @return 算力信息
     */
    public static ComputingPowerDTO getUserComputingPowerInfo(String walletAddress) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        SingleResponse<ComputingPowerDTO> response = computingPowerService.getComputingPowerInfo(walletAddress, today);
        return response.isSuccess() ? response.getData() : null;
    }

    /**
     * 获取用户完整算力信息（指定日期）
     * @param walletAddress 钱包地址
     * @param dayTime 日期
     * @return 算力信息
     */
    public static ComputingPowerDTO getUserComputingPowerInfo(String walletAddress, String dayTime) {
        SingleResponse<ComputingPowerDTO> response = computingPowerService.getComputingPowerInfo(walletAddress, dayTime);
        return response.isSuccess() ? response.getData() : null;
    }

    /**
     * 检查用户是否有算力
     * @param walletAddress 钱包地址
     * @return 是否有算力
     */
    public static boolean hasComputingPower(String walletAddress) {
        BigDecimal selfPower = getUserSelfPower(walletAddress);
        return selfPower.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 检查用户是否有推荐算力
     * @param walletAddress 钱包地址
     * @return 是否有推荐算力
     */
    public static boolean hasRecommendPower(String walletAddress) {
        BigDecimal recommendPower = getUserRecommendPower(walletAddress);
        return recommendPower.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 检查用户是否有直推算力
     * @param walletAddress 钱包地址
     * @return 是否有直推算力
     */
    public static boolean hasDirectRecommendPower(String walletAddress) {
        BigDecimal directPower = getUserDirectRecommendPower(walletAddress);
        return directPower.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 检查用户是否有小区算力
     * @param walletAddress 钱包地址
     * @return 是否有小区算力
     */
    public static boolean hasMinPower(String walletAddress) {
        BigDecimal minPower = getUserMinPower(walletAddress);
        return minPower.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 检查用户是否有新增算力
     * @param walletAddress 钱包地址
     * @return 是否有新增算力
     */
    public static boolean hasNewPower(String walletAddress) {
        BigDecimal newPower = getUserNewPower(walletAddress);
        return newPower.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 计算算力占比
     * @param userPower 用户算力
     * @param totalPower 总算力
     * @return 占比（0-1之间）
     */
    public static BigDecimal calculatePowerRatio(BigDecimal userPower, BigDecimal totalPower) {
        if (totalPower.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return userPower.divide(totalPower, 8, BigDecimal.ROUND_HALF_DOWN);
    }

    /**
     * 根据算力占比计算奖励
     * @param userPower 用户算力
     * @param totalPower 总算力
     * @param totalReward 总奖励
     * @return 用户应得奖励
     */
    public static BigDecimal calculateRewardByPower(BigDecimal userPower, BigDecimal totalPower, BigDecimal totalReward) {
        BigDecimal ratio = calculatePowerRatio(userPower, totalPower);
        return ratio.multiply(totalReward);
    }
}