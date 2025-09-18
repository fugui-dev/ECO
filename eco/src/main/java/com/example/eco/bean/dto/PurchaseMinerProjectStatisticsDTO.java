package com.example.eco.bean.dto;

import lombok.Data;

@Data
public class PurchaseMinerProjectStatisticsDTO {

    /**
     * 总矿池算力
     */
    private String totalComputingPower;

    /**
     * 总矿机数量
     */
    private Integer totalPurchaseMinerProjectCount;

    /**
     * 昨日新增矿池算力
     */
    private String yesterdayTotalComputingPower;

    /**
     * 昨日新增矿机数量
     */
    private Integer yesterdayTotalPurchaseMinerProjectCount;

    /**
     * 累计ECO挖矿数量
     */
    private String totalEcoNumber;

    /**
     * 累计挖矿进度
     */
    private String progress;

    /**
     * 今日ECO价格
     */
    private String price;

    /**
     *  昨日挖矿数量
     */
    private String yesterdayTotalEcoNumber;
}
