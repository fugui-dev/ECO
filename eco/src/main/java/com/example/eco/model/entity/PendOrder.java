package com.example.eco.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
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
    private String order;

    /**
     * 钱包地址
     */
    private String walletAddress;

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
     * 转账凭证图片列表,逗号分隔
     */
    private String imageList;


    /**
     * 备注
     */
    private String remark;

    /**
     * 下单时间
     */
    private String placeOrderTime;

    /**
     * 确认时间
     */
    private Long confirmTime;


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
