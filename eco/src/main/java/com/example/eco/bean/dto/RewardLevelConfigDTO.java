package com.example.eco.bean.dto;

import lombok.Data;

@Data
public class RewardLevelConfigDTO {

    private Integer id;

    /**
     * 等级
     */
    private Integer level;

    /**
     * 奖励比例
     */
    private String rewardRate;

    private Long createTime;

    private Long updateTime;

}
