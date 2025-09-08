package com.example.eco.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
@TableName("account_transaction")
public class AccountTransaction {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 交易类型
     */
    private String transactionType;

    /**
     * 交易数量
     */
    private String amount;

    /**
     * 交易时间
     */
    private Long transactionTime;


    /**
     * 交易哈希
     */
    private String hash;

    /**
     * 状态
     */
    private String status;

    /**
     * 备注
     */
    private String remark;
}
