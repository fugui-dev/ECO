package com.example.eco.bean.dto;

import lombok.Data;

@Data
public class PurchaseMinerProjectRewardDTO {

    /**
     * 静态收益
     */
    private String staticReward;

    /**
     * 动态收益
     */
    private String dynamicReward;

    /**
     * 推荐奖励
     */
    private String recommendReward;

    /**
     * 小区奖励
     */
    private String baseReward;

    /**
     * 新增奖励
     */
    private String newReward;
}
