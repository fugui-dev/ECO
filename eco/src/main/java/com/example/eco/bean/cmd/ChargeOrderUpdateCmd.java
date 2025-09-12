package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class ChargeOrderUpdateCmd {

    /**
     * 状态
     */
    private String status;

    /**
     * 交易哈希
     */
    private String hash;
}
