package com.example.eco.bean.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

@Data
public class PendOrderAppealDTO {

    private Integer id;

    /**
     * 订单号
     */
    private String order;

    /**
     * 挂单id
     */
    private Integer pendOrderId;

    /**
     * 钱包地址
     */
    private String walletAddress;


    /**
     * 申诉状态 0-未处理 1-已处理
     */
    private String status;


    private String statusName;


    /**
     * 申诉内容
     */
    private String content;


    /**
     * 拒绝原因
     */
    private String reason;


    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 更新时间
     */
    private Long updateTime;
}
