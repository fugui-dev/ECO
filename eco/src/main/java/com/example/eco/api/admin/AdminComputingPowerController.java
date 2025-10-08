package com.example.eco.api.admin;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.dto.ComputingPowerDTO;
import com.example.eco.core.service.ComputingPowerService;
import com.example.eco.core.service.impl.ComputingPowerServiceImplV2;
import com.example.eco.util.ComputingPowerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 算力管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/computing-power")
public class AdminComputingPowerController {

    @Resource(name = "computingPowerService")
    private ComputingPowerService computingPowerService;
    @Resource
    private ComputingPowerServiceImplV2 computingPowerServiceV2;

    /**
     * 获取用户算力信息
     */
    @GetMapping("/user/{walletAddress}")
    public SingleResponse<ComputingPowerDTO> getUserComputingPower(
            @PathVariable String walletAddress,
            @RequestParam(required = false) String dayTime) {
        
        if (dayTime == null || dayTime.trim().isEmpty()) {
            dayTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }

        return computingPowerService.getComputingPowerInfo(walletAddress, dayTime);
    }

    /**
     * 获取总算力
     */
    @GetMapping("/total")
    public SingleResponse<BigDecimal> getTotalPower(
            @RequestParam(required = false) String dayTime) {
        
        if (dayTime == null || dayTime.trim().isEmpty()) {
            dayTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }

        return computingPowerService.calculateTotalPower(dayTime);
    }

    /**
     * 获取用户自身算力
     */
    @GetMapping("/user/{walletAddress}/self")
    public SingleResponse<BigDecimal> getUserSelfPower(
            @PathVariable String walletAddress,
            @RequestParam(required = false) String dayTime) {
        
        if (dayTime == null || dayTime.trim().isEmpty()) {
            dayTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }

        return computingPowerService.calculateUserSelfPower(walletAddress, dayTime);
    }

    /**
     * 获取用户推荐算力
     */
    @GetMapping("/user/{walletAddress}/recommend")
    public SingleResponse<BigDecimal> getUserRecommendPower(
            @PathVariable String walletAddress,
            @RequestParam(required = false) String dayTime) {
        
        if (dayTime == null || dayTime.trim().isEmpty()) {
            dayTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }

        return computingPowerService.calculateUserRecommendPower(walletAddress, dayTime);
    }

    /**
     * 获取用户直推算力
     */
    @GetMapping("/user/{walletAddress}/direct")
    public SingleResponse<BigDecimal> getUserDirectPower(
            @PathVariable String walletAddress,
            @RequestParam(required = false) String dayTime) {
        
        if (dayTime == null || dayTime.trim().isEmpty()) {
            dayTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }

        return computingPowerService.calculateUserDirectRecommendPower(walletAddress, dayTime);
    }

    /**
     * 获取用户小区算力
     */
    @GetMapping("/user/{walletAddress}/min")
    public SingleResponse<BigDecimal> getUserMinPower(
            @PathVariable String walletAddress,
            @RequestParam(required = false) String dayTime) {
        
        if (dayTime == null || dayTime.trim().isEmpty()) {
            dayTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }

        return computingPowerService.calculateUserMinPower(walletAddress, dayTime);
    }

    /**
     * 获取用户新增算力
     */
    @GetMapping("/user/{walletAddress}/new")
    public SingleResponse<BigDecimal> getUserNewPower(
            @PathVariable String walletAddress,
            @RequestParam(required = false) String dayTime) {
        
        if (dayTime == null || dayTime.trim().isEmpty()) {
            dayTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }

        return computingPowerService.calculateUserNewPower(walletAddress, dayTime);
    }

    /**
     * 使用工具类获取算力信息（示例）
     */
    @GetMapping("/util/user/{walletAddress}")
    public SingleResponse<ComputingPowerDTO> getUserComputingPowerByUtil(@PathVariable String walletAddress) {
        ComputingPowerDTO dto = ComputingPowerUtil.getUserComputingPowerInfo(walletAddress);
        return dto != null ? SingleResponse.of(dto) : SingleResponse.buildFailure("获取算力信息失败");
    }

    /**
     * 使用工具类获取总算力（示例）
     */
    @GetMapping("/util/total")
    public SingleResponse<BigDecimal> getTotalPowerByUtil() {
        BigDecimal totalPower = ComputingPowerUtil.getTodayTotalPower();
        return SingleResponse.of(totalPower);
    }

    /**
     * 清除用户算力缓存
     */
    @PostMapping("/cache/user/{walletAddress}/clear")
    public SingleResponse<Void> clearUserCache(@PathVariable String walletAddress) {
        computingPowerServiceV2.invalidateUserCache(walletAddress);
        return SingleResponse.buildSuccess();
    }

    /**
     * 清除指定日期的算力缓存
     */
    @PostMapping("/cache/day/{dayTime}/clear")
    public SingleResponse<Void> clearDayCache(@PathVariable String dayTime) {
        computingPowerServiceV2.invalidateDayCache(dayTime);
        return SingleResponse.buildSuccess();
    }

    /**
     * 算力统计信息
     */
    @GetMapping("/statistics")
    public SingleResponse<Object> getStatistics(@RequestParam(required = false) String dayTime) {
        if (dayTime == null || dayTime.trim().isEmpty()) {
            dayTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }

        try {
            // 获取总算力
            SingleResponse<BigDecimal> totalPowerResponse = computingPowerService.calculateTotalPower(dayTime);
            BigDecimal totalPower = totalPowerResponse.isSuccess() ? totalPowerResponse.getData() : BigDecimal.ZERO;

            // 使用工具类获取当前总算力
            BigDecimal todayTotalPower = ComputingPowerUtil.getTodayTotalPower();

            // 构建统计信息
            List<Object> statistics = new ArrayList<>();
            statistics.add("总算力: " + totalPower);
            statistics.add("今日总算力: " + todayTotalPower);
            statistics.add("计算时间: " + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            return SingleResponse.of(statistics);

        } catch (Exception e) {
            log.error("获取算力统计失败", e);
            return SingleResponse.buildFailure("获取统计信息失败: " + e.getMessage());
        }
    }
}