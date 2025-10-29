package com.example.eco.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 算力奖励档位记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("power_reward_level")
public class PowerRewardLevel {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 日期
     */
    private String dayTime;

    /**
     * 算力
     */
    private String computingPower;

    /**
     * 档位级别
     */
    private Integer level;

    /**
     * 奖励数量
     */
    private String rewardAmount;

    /**
     * 每档算力差距
     */
    private String basePower;

    /**
     * 每档奖励数量
     */
    private String levelAddSize;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 更新时间
     */
    private Long updateTime;
}

