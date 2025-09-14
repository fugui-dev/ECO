package com.example.eco.bean.dto;

import lombok.Data;

@Data
public class RecommendComputingPowerDTO {

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 总算力
     */
    private String totalComputingPower;

    /**
     * 最小算力
     */
    private String minComputingPower;

    /**
     * 最小算力对应的钱包地址
     */
    private String minWalletAddress;

    /**
     * 最大算力
     */
    private String maxComputingPower;

    /**
     * 最大算力对应的钱包地址
     */
    private String maxWalletAddress;

    /**
     * 直推人数
     */
    private Integer directRecommendCount;
}
