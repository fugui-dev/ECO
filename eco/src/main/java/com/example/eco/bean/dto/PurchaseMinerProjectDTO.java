package com.example.eco.bean.dto;

import lombok.Data;

@Data
public class PurchaseMinerProjectDTO {

    private Integer id;

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
     * ESG数量
     */
    private String esgNumber;

    /**
     * ECO数量
     */
    private String ecoNumber;


    /**
     * 购买方式类型
     */
    private String type;

    private String typeName;

    /**
     * 状态
     */
    private String status;

    private String statusName;

    /**
     * 总ECO奖励
     */
    private String totalReward;

    /**
     * 昨天总ECO奖励
     */
    private String yesterdayTotalReward;

    /**
     * 失败原因
     */
    private String reason;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 完成时间
     */
    private Long finishTime;
}
