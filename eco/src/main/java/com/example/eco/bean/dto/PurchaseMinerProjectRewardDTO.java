package com.example.eco.bean.dto;

import lombok.Data;

@Data
public class PurchaseMinerProjectRewardDTO {

    /**
     * 静态收益
     */
    private String staticReward;

    /**
     * 静态收益价格
     */
    private String staticRewardPrice;

    /**
     * 动态收益
     */
    private String dynamicReward;

    /**
     * 动态收益价格
     */
    private String dynamicRewardPrice;

    /**
     * 推荐奖励
     */
    private String recommendReward;

    /**
     * 推荐奖励价格
     */
    private String recommendRewardPrice;

    /**
     * 小区奖励
     */
    private String baseReward;

    /**
     * 小区奖励价格
     */
    private String baseRewardPrice;

    /**
     * 新增奖励
     */
    private String newReward;

    /**
     * 新增奖励价格
     */
    private String newRewardPrice;
}
