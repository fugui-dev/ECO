package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class RewardLevelConfigCreateCmd {

    /**
     * 等级
     */
    private Integer level;

    /**
     * 奖励比例
     */
    private String rewardRate;

}
