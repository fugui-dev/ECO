package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class AccountWithdrawServiceCmd {

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 数量
     */
    private String number;

    /**
     * 订单
     */
    private String order;
}
