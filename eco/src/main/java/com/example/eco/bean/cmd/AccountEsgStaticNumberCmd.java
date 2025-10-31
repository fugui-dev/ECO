package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class AccountEsgStaticNumberCmd {

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 数量
     */
    private String number;

    /**
     * 订单号
     */
    private String order;

}
