package com.example.eco.bean.dto;

import lombok.Data;

@Data
public class MinerProjectDTO {

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
     * 矿机限额
     */
    private String quota;


    /**
     * 已使用ESG数量
     */
    private String amount;

    /**
     * 是否达到ESG限额
     */
    private Boolean disable;


    private Long createTime;


    private Long updateTime;
}
