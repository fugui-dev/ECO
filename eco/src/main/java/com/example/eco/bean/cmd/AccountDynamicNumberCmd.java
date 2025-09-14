package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class AccountDynamicNumberCmd {

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 账号类型
     */
    private String type;

    /**
     * 数量
     */
    private String number;


    private String order;

}
