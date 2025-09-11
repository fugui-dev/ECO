package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class PendOrderCancelCmd {

    /**
     * 订单号
     */
    private String order;

    /**
     * 钱包地址
     */
    private String walletAddress;


}
