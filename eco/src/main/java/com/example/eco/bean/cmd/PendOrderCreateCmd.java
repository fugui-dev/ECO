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
     * 收款钱包
     */
    private String recipientWalletAddress;


}
