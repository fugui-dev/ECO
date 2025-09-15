package com.example.eco.bean.dto;

import lombok.Data;

import java.util.List;

@Data
public class RecommendStatisticsLogDTO {

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 直接推荐人数
     */
    private Integer directRecommendCount;

    /**
     * 总算力
     */
    private String  totalComputingPower;

    /**
     * 总直接推荐算力
     */
    private String totalDirectRecommendComputingPower;

    /**
     * 总推荐算力
     */
    private String totalRecommendComputingPower;

    /**
     * 最小算力
     */
    private String minComputingPower;


    /**
     * 最大算力
     */
    private String maxComputingPower;

    /**
     * 新增算力
     */
    private String newComputingPower;


}
