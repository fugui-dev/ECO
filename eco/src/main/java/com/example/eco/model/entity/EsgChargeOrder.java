package com.example.eco.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("esg_charge_order")
public class EsgChargeOrder {

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
     * 数量
     */
    private String number;

    /**
     * 状态
     */
    private String status;

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
