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
     * 实际到账算力
     */
    private String realComputingPower;

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
     * 失败原因
     */
    private String reason;

    /**
     * 创建时间
     */
    private Long createTime;
}
