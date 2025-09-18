package com.example.eco.bean.dto;

import lombok.Data;

@Data
public class RewardStatisticsLogDTO {

    private Integer id;

    /**
     * 总奖励数
     */
    private String totalReward;

    /**
     * 静态收益
     */
    private String totalStaticReward;

    /**
     * 动态收益
     */
    private String totalDynamicReward;

    /**
     * 推荐奖励
     */
    private String totalRecommendReward;

    /**
     * 小区奖励
     */
    private String totalBaseReward;

    /**
     * 新增奖励
     */
    private String totalNewReward;

    /**
     * 日期
     */
    private String dayTime;
}
