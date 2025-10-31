package com.example.eco.bean.dto;

import lombok.Data;

@Data
public class EsgMinerProjectDTO {

    private Integer id;

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

    /**
     * 矿机状态 1 开启 0 关闭
     */
    private Integer status;


    private Long createTime;


    private Long updateTime;
}
