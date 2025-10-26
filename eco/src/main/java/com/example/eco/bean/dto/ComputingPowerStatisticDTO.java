package com.example.eco.bean.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ComputingPowerStatisticDTO {

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 空投矿机数量
     */
    private Long airdropMinerCount;

    /**
     * ESG矿机数量
     */
    private Long esgMinerCount;

    /**
     * ECO矿机数量
     */
    private Long ecoMinerCount;

    /**
     * ECO数量
     */
    private BigDecimal ecoNumber;

    /**
     * ESG数量
     */
    private BigDecimal esgNumber;

    /**
     * ECO+ESG矿机数量
     */
    private Long ecoEsgMinerCount;

    /**
     * 自身算力
     */
    private BigDecimal selfComputingPower;

    /**
     * 推荐算力
     */
    private BigDecimal recommendComputingPower;

    /**
     * 直推算力
     */
    private BigDecimal directRecommendComputingPower;

    /**
     * 小区算力
     */
    private BigDecimal minComputingPower;

    /**
     * 最大算力
     */
    private BigDecimal maxComputingPower;


}
