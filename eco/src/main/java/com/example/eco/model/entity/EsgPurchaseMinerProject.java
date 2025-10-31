package com.example.eco.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 购买矿机项目
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("esg_purchase_miner_project")
public class EsgPurchaseMinerProject {


    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 订单号
     */
    @TableField(value = "`order`")
    private String order;

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 矿机项目ID
     */
    private Integer minerProjectId;

    /**
     * 价格
     */
    private String price;

    /**
     * 矿机算力
     */
    private String computingPower;

    /**
     * 状态
     */
    private String status;

    /**
     * 产生奖励数量
     */
    private String reward;

    /**
     * 昨天产生奖励数量
     */
    private String yesterdayReward;

    /**
     * 失败原因
     */
    private String reason;

    /**
     * 创建时间
     */
    private Long createTime;


    private Long updateTime;

    /**
     * 完成时间
     */
    private Long finishTime;
}
