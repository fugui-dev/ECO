package com.example.eco.bean.dto;

import lombok.Data;

@Data
public class WithdrawRecordDTO {

    private Integer id;

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 订单号
     */
    private String order;

    /**
     * 账号类型
     */
    private String type;

    /**
     * 账号类型名称
     */
    private String typeName;

    /**
     * 提现数量
     */
    private String withdrawNumber;

    /**
     * 提现时间
     */
    private Long withdrawTime;

    /**
     * 审核时间
     */
    private Long reviewTime;

    /**
     * 提现状态
     */
    private String status;

    /**
     * 提现状态名称
     */
    private String statusName;

    /**
     * 审核拒绝原因
     */
    private String reason;

    /**
     * 备注
     */
    private String remark;


    private Long createTime;

    private Long updateTime;
}
