package com.example.eco.bean.dto;

import lombok.Data;

@Data
public class ChargeOrderDTO {

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
     * 类型
     */
    private String type;

    /**
     * 类型名称
     */
    private String typeName;

    /**
     * 数量
     */
    private String number;

    /**
     * 单价
     */
    private String price;

    /**
     * 总价
     */
    private String totalPrice;

    /**
     * 状态
     */
    private String status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 交易哈希
     */
    private String hash;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 完成时间
     */
    private Long finishTime;
}
