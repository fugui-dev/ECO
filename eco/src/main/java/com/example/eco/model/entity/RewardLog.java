package com.example.eco.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
@TableName("reward_log")
public class RewardLog {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 奖励类型（STATIC/DYNAMIC）
     */
    private String rewardType;

    /**
     * 动态奖励子类型（RECOMMEND/BASE/NEW）
     */
    private String dynamicRewardType;

    /**
     * 计算奖励数量
     */
    private String calculatedReward;

    /**
     * 实际发放奖励数量
     */
    private String actualReward;

    /**
     * 舍去的奖励数量
     */
    private String discardedReward;

    /**
     * 舍去原因
     */
    private String discardReason;

    /**
     * 矿机ID（静态奖励时使用）
     */
    private Integer minerId;

    /**
     * 算力值
     */
    private String computingPower;

    /**
     * 总算力值
     */
    private String totalComputingPower;

    /**
     * 订单号
     */
    @TableField(value = "`order`")
    private String order;


    /**
     * 奖励发放日期
     */
    private String dayTime;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 更新时间
     */
    private Long updateTime;
}