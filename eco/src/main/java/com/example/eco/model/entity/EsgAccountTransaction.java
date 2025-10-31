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
@TableName("esg_account_transaction")
public class EsgAccountTransaction {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 账号ID
     */
    private Integer accountId;


    /**
     * 交易类型
     */
    private String transactionType;

    /**
     * 交易数量
     */
    private String number;

    /**
     * 交易前余额
     */
    private String beforeNumber;

    /**
     * 交易后余额
     */
    private String afterNumber;

    /**
     * 交易时间
     */
    private Long transactionTime;


    /**
     * 交易哈希
     */
    private String hash;

    /**
     * 订单
     */
    @TableField(value = "`order`")
    private String order;

    /**
     * 状态
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

}
