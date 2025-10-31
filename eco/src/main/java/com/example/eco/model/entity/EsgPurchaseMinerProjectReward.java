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
@TableName("esg_purchase_miner_project_reward")
public class EsgPurchaseMinerProjectReward {

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
    private Integer esgPurchaseMinerProjectId;

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 奖励
     */
    private String reward;


    /**
     * 矿机算力
     */
    private String computingPower;


    /**
     * 矿机挖矿速率
     */
    private String rate;

    /**
     * 奖励时间
     */
    private String dayTime;


    private Long createTime;

    private Long updateTime;

}
