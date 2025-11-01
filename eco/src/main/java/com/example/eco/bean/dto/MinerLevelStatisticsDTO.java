package com.example.eco.bean.dto;

import lombok.Data;

/**
 * 矿机等级统计DTO
 */
@Data
public class MinerLevelStatisticsDTO {

    /**
     * 矿机项目ID
     */
    private Integer minerProjectId;

    /**
     * 矿机价格
     */
    private String price;

    /**
     * 矿机算力
     */
    private String computingPower;

    /**
     * 购买数量
     */
    private Integer count;
}
