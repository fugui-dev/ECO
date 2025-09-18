package com.example.eco.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 挂单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("pend_order")
public class PendOrder {

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
     * 电报
     */
    private String telegram;

    /**
     * 微信
     */
    private String wechat;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;


    /**
     * 收款钱包
     */
    private String recipientWalletAddress;

    /**
     * 挂单类型
     */
    private String type;

    /**
     * 挂单数量
     */
    private String number;

    /**
     * 挂单单价
     */
    private String price;

    /**
     * 挂单总价
     */
    private String totalPrice;

    /**
     * 状态
     */
    private String status;

    /**
     * 买家钱包地址
     */
    private String buyerWalletAddress;



    /**
     * 上传凭证
     */
    private String voucher;


    /**
     * 备注
     */
    private String remark;

    /**
     * 下单时间
     */
    private Long placeOrderTime;

    /**
     * 确认时间
     */
    private Long confirmTime;

    /**
     * 取消时间
     */
    private Long cancelTime;


    @Version
    private Long version;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 更新时间
     */
    private Long updateTime;
}
