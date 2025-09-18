package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class PendOrderCreateCmd {


    /**
     * 钱包地址
     */
    private String walletAddress;

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

    /**
     * 电报
     */
    private String telegram;

    /**
     * 微信
     */
    private String wechat;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 收款钱包
     */
    private String recipientWalletAddress;


}
