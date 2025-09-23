package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class PurchaseMinerProjectRewardQry {

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 日期
     */
    private String dayTime;
}
