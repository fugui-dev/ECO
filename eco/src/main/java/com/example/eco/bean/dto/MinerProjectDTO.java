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


    private Long createTime;


    private Long updateTime;
}
