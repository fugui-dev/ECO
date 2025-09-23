package com.example.eco.bean.cmd;

import lombok.Data;

/**
 * 账户添加
 */
@Data
public class AccountAddCmd {

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
