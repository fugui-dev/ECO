package com.example.eco.bean.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

@Data
public class EsgPurchaseMinerProjectDTO {

    private Integer id;

    /**
     * 订单号
     */
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


    private String statusName;

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
