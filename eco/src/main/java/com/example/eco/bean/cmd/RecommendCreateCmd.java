package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class RecommendCreateCmd {

    /**
     * 推荐人钱包地址
     */
    private String walletAddress;

    /**
     * 被推荐人钱包地址
     */
    private String recommendWalletAddress;

}
