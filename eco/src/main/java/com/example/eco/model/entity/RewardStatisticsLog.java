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
@TableName("reward_statistics_log")
public class RewardStatisticsLog {

    @TableId(type = IdType.AUTO)
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
