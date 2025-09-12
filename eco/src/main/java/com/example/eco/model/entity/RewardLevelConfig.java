package com.example.eco.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("reward_level_config")
public class RewardLevelConfig {

    @TableId(type = IdType.AUTO)
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
