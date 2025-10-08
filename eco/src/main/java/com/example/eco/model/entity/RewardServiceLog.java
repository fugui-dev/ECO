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
@TableName("reward_service_log")
public class RewardServiceLog {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 日期
     */
    private String dayTime;

    /**
     * 奖励总数
     */
    private String reward;

    /**
     * ECO手续费
     */
    private String ecoNumber;

    /**
     * ESG手续费
     */
    private String esgNumber;

    /**
     * 订单号
     */
    @TableField(value = "`order`")
    private String order;


    private String status;


    private String reason;
}
