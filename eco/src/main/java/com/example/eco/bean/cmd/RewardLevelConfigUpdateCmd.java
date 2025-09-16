package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class RewardLevelConfigUpdateCmd {

    private Integer id;

    /**
     * 奖励比例
     */
    private String rewardRate;
}
