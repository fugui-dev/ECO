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
@TableName("reward_statistics")
public class RewardStatistics {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 总奖励数量
     */
    private String totalReward;


    private String totalRecommendReward;



}
