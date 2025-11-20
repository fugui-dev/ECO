package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class PurchaseMinerProjectRewardStatisticQry {

    private String rewardType;

    /**
     * 最小奖励数量
     */
    private Integer minRewardNumber;

    /**
     * 最大奖励数量
     */
    private Integer maxRewardNumber;
}
