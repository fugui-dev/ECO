package com.example.eco.bean.dto;

import lombok.Data;

@Data
public class RecommendRecordDTO {
    private Integer id;

    /**
     * 被推荐人的钱包地址
     */
    private String walletAddress;

    /**
     * 推荐人的钱包地址
     */
    private String recommendWalletAddress;

    /**
     * 推荐时间
     */
    private Long recommendTime;

}
