package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class withdrawRecordCancelCmd {

    private Integer id;

    /**
     * 钱包地址
     */
    private String walletAddress;
}
