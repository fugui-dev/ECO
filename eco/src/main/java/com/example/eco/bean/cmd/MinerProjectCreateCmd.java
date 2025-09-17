package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class MinerProjectCreateCmd {

    /**
     * 价格
     */
    private String price;

    /**
     * 矿机算力
     */
    private String computingPower;

    /**
     * 矿机限额
     */
    private String quota;
}
