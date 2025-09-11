package com.example.eco.bean.dto;

import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

@Data
public class PendOrderDTO {

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
     * 挂单类型名称
     */
    private String typeName;

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
     * 状态名称
     */
    private String statusName;

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
