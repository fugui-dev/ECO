package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class AccountReleaseLockSellNumberCmd {
    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 订单
     */
    private String order;


}
