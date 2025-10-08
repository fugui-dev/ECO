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
@TableName("purchase_miner_project")
public class PurchaseMinerProject {


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
     * 实际到账矿机算力
     */
    private String actualComputingPower;

    /**
     * 加速到期时间
     */
    private Long accelerateExpireTime;

    /**
     * 购买方式类型
     */
    private String type;

    /**
     * 状态
     */
    private String status;


    /**
     * 花费ESG数量
     */
    private String esgNumber;

    /**
     * 花费ECO数量
     */
    private String ecoNumber;

    /**
     * 产生奖励数量
     */
    private String reward;

    /**
     * 产生奖励的总价值
     */
    private String rewardPrice;

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
