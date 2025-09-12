package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class AccountLockChargeNumberCmd {
    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 订单
     */
    private String order;

    /**
     * 交易哈希
     */
    private String hash;
}
