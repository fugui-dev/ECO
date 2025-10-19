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

    /**
     * 是否开启ESG抢购模式
     * 0-关闭，1-开启
     */
    private Integer esgRushMode;

    /**
     * ESG抢购数量限制
     * 当esgRushMode=1时生效，表示每日ESG抢购数量限制
     */
    private Integer esgRushLimit;
}
