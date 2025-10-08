package com.example.eco.bean.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 算力信息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComputingPowerDTO {

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 自身算力
     */
    private BigDecimal selfPower;

    /**
     * 总算力（自身+下级）
     */
    private BigDecimal totalPower;

    /**
     * 推荐算力（下级算力总和）
     */
    private BigDecimal recommendPower;

    /**
     * 直推算力（直接下级算力总和）
     */
    private BigDecimal directRecommendPower;

    /**
     * 小区算力（除最大算力外的直推算力总和）
     */
    private BigDecimal minPower;

    /**
     * 最大算力（直推中最大算力）
     */
    private BigDecimal maxPower;

    /**
     * 最大算力用户地址
     */
    private String maxPowerWalletAddress;

    /**
     * 新增算力（当日新增）
     */
    private BigDecimal newPower;

    /**
     * 直推人数
     */
    private Integer directRecommendCount;

    /**
     * 推荐层级
     */
    private Integer level;
}