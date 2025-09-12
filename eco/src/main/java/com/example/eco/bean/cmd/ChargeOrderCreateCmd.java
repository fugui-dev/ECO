package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class ChargeOrderCreateCmd {

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 类型
     */
    private String type;

    /**
     * 数量
     */
    private String number;

    /**
     * 单价
     */
    private String price;

    /**
     * 总价
     */
    private String totalPrice;

    /**
     * 哈希值
     */
    private String hash;
}
