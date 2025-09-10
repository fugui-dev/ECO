package com.example.eco.bean.cmd;

import com.example.eco.bean.PageQuery;
import lombok.Data;

@Data
public class AccountTransactionPageQry extends PageQuery {

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 交易类型
     */
    private String transactionType;

    /**
     * 交易状态
     */
    private String transactionStatus;

    /**
     * 账号类型
     */
    private String accountType;

    /**
     * 订单ID
     */
    private Integer orderId;
}
