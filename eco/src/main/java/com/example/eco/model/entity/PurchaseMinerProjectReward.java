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
@TableName("purchase_miner_project_reward")
public class PurchaseMinerProjectReward {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 订单
     */
    @TableField(value = "`order`")
    private String order;

    /**
     * 购买矿机项目ID
     */
    private Integer purchaseMinerProjectId;

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 推荐人钱包地址
     */
    private String recommendWalletAddress;


    /**
     * 团队长钱包地址
     */
    private String leaderWalletAddress;

    /**
     * 本钱包算力
     */
    private String computingPower;

    /**
     * 小区算力
     */
    private String minComputingPower;

    /**
     * 大区算力
     */
    private String maxComputingPower;

    /**
     * 全网总算力
     */
    private String totalComputingPower;

    /**
     * 奖励
     */
    private String reward;

    /**
     * 奖励价值
     */
    private String rewardPrice;

    /**
     * 类型
     */
    private String type;

    /**
     * 奖励类型
     */
    private String rewardType;


    /**
     * 奖励时间
     */
    private String dayTime;


    private Long createTime;

    private Long updateTime;
}
