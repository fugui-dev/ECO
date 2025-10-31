package com.example.eco.bean.dto;

import lombok.Data;

@Data
public class EsgAccountTransactionDTO {

    private Integer id;

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 账号ID
     */
    private Integer accountId;

    /**
     * 交易类型
     */
    private String transactionType;

    /**
     * 交易类型名称
     */
    private String transactionTypeName;

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
     * 订单ID
     */
    private String order;

    /**
     * 状态
     */
    private String status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 备注
     */
    private String remark;

}
