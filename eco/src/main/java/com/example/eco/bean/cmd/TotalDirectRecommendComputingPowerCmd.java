package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class TotalDirectRecommendComputingPowerCmd {

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 矿机算力
     */
    private String computingPower;
}
