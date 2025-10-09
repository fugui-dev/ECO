package com.example.eco.bean.dto;

import lombok.Data;

/**
 * Etherscan代币转账DTO
 */
@Data
public class EtherScanTokenTransferDTO {

    /**
     * 交易哈希
     */
    private String hash;

    /**
     * 代币合约地址
     */
    private String contractAddress;

    /**
     * 代币名称
     */
    private String tokenName;

    /**
     * 代币符号
     */
    private String tokenSymbol;

    /**
     * 代币精度
     */
    private String tokenDecimal;

    /**
     * 发送方地址
     */
    private String from;

    /**
     * 接收方地址
     */
    private String to;

    /**
     * 转账金额（原始值）
     */
    private String value;

    /**
     * 区块号
     */
    private String blockNumber;

    /**
     * 区块哈希
     */
    private String blockHash;

    /**
     * 时间戳
     */
    private String timeStamp;

    /**
     * 交易索引
     */
    private String transactionIndex;

    /**
     * Gas使用量
     */
    private String gasUsed;

    /**
     * Gas价格
     */
    private String gasPrice;

    /**
     * 是否有错误
     */
    private String isError;

    /**
     * 确认数
     */
    private String confirmations;
}
