package com.example.eco.bean.cmd;

import lombok.Data;

/**
 * 账户扣除
 */
@Data
public class AccountDeductCmd {

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 账户类型
     */
    private String accountType;

    /**
     * 扣除金额
     */
    private String number;

    /**
     * 订单
     */
    private String order;


}
