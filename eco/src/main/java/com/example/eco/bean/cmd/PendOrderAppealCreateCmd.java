package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class PendOrderAppealCreateCmd {

    /**
     * 订单号
     */
    private String order;

    /**
     * 钱包地址
     */
    private String walletAddress;


    /**
     * 申诉内容
     */
    private String content;
}
