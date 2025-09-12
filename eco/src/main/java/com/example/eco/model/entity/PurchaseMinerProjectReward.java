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
@TableName("purchase_miner_project_reward")
public class PurchaseMinerProjectReward {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 订单
     */
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
     * 关联钱包地址
     */
    private String relationWalletAddress;

    /**
     * 团队长钱包地址
     */
    private String leaderWalletAddress;

    /**
     * 算力
     */
    private String computingPower;

    /**
     * 小区新增算力
     */
    private String newAreaComputingPower;

    /**
     * 小区算力
     */
    private String minAreaComputingPower;

    /**
     * 小区钱包地址
     */
    private String minAreaChildAddress;

    /**
     * 总算力
     */
    private String totalAreaComputingPower;

    /**
     * 全网总算力
     */
    private String totalComputingPower;

    /**
     * 奖励
     */
    private String reward;

    /**
     * 奖励比例
     */
    private String rewardRate;

    /**
     * 等级
     */
    private Integer level;

    /**
     * 类型
     */
    private String type;

    /**
     * 奖励类型
     */
    private String rewardType;

    /**
     * 状态
     */
    private String status;

    /**
     * 奖励时间
     */
    private String dayTime;


    private Long createTime;

    private Long updateTime;
}
