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
@TableName("pend_order_appeal")
public class PendOrderAppeal {

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
     * 申诉状态 0-未处理 1-已处理
     */
    private String status;


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
