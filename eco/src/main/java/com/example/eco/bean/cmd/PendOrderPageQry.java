package com.example.eco.bean.cmd;

import com.example.eco.bean.PageQuery;
import lombok.Data;

@Data
public class PendOrderPageQry extends PageQuery {

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 买家钱包地址
     */
    private String buyerWalletAddress;

    /**
     * 状态
     */
    private String status;

    /**
     * 挂单类型
     */
    private String type;
}
