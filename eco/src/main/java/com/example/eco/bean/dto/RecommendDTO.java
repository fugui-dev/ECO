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
     * 等级
     */
    private Integer level;

    /**
     * 状态
     */
    private String status;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 更新时间
     */
    private Long updateTime;
}
