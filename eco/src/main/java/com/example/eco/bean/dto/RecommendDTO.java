package com.example.eco.bean.dto;

import lombok.Data;

@Data
public class RecommendDTO {

    private Integer id;

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 推荐人钱包地址
     */
    private String recommendWalletAddress;

    /**
     * 领导钱包地址
     */
    private String leaderWalletAddress;

    /**
     * 创建时间
     */
    private Long createTime;
}
