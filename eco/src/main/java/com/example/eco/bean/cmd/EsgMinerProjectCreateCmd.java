package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class EsgMinerProjectCreateCmd {

    /**
     * 价格
     */
    private String price;

    /**
     * 矿机算力
     */
    private String computingPower;

    /**
     * 矿机挖矿速率
     */
    private String rate;

}
