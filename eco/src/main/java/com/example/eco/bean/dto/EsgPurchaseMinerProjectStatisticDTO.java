package com.example.eco.bean.dto;

import lombok.Data;

@Data
public class EsgPurchaseMinerProjectStatisticDTO {

    /**
     * 矿机总数
     */
    private Integer totalMinerCount;

    /**
     * 总算力
     */
    private String totalComputingPower;

    /**
     * 昨日总算力
     */
    private String yesterdayComputingPower;

    /**
     * 昨日新增矿机数
     */
    private Integer yesterdayNewMinerCount;
}
