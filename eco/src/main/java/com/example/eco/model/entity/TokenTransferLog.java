package com.example.eco.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 代币转账记录实体类
 */
@Data
@TableName("token_transfer_log")
public class TokenTransferLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 交易哈希
     */
    @TableField("tx_hash")
    private String hash;

    /**
     * 代币合约地址
     */
    private String tokenAddress;

    /**
     * 代币类型 (ESG/ECO)
     */
    private String tokenType;

    /**
     * 发送方地址
     */
    private String fromAddress;

    /**
     * 接收方地址
     */
    private String toAddress;

    /**
     * 转账金额
     */
    private String transferValue;

    /**
     * 区块号
     */
    private Long blockNumber;

    /**
     * 交易索引
     */
    private Integer transactionIndex;

    /**
     * Gas使用量
     */
    private String gasUsed;

    /**
     * 交易状态 (SUCCESS/FAILED/PENDING)
     */
    private String status;

    /**
     * 是否已检查
     */
    private Boolean checked;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 更新时间
     */
    private Long updateTime;
}