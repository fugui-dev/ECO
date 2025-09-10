package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class AccountTransactionCreateCmd {

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 账号ID
     */
    private Integer accountId;

    /**
     * 账号类型
     */
    private String accountType;

    /**
     * 交易类型
     */
    private String transactionType;

    /**
     * 交易数量
     */
    private String number;

    /**
     * 交易前余额
     */
    private String beforeNumber;

    /**
     * 交易后余额
     */
    private String afterNumber;

    /**
     * 交易时间
     */
    private Long transactionTime;


    /**
     * 交易哈希
     */
    private String hash;

    /**
     * 订单
     */
    private String order;

    /**
     * 状态
     */
    private String status;

    /**
     * 备注
     */
    private String remark;
}
