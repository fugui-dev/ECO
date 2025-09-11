package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class PendOrderCreateCmd {

    /**
     * 订单号
     */
    private String order;

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 联系方式
     */
    private String contactWay;

    /**
     * 挂单类型
     */
    private String type;

    /**
     * 挂单数量
     */
    private String number;

    /**
     * 挂单单价
     */
    private String price;

    /**
     * 挂单总价
     */
    private String totalPrice;

}
