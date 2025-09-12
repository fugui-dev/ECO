package com.example.eco.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class EtherScanAccountTransaction {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Long blockNumber;

    private String blockHash;

    private String timeStamp;
    @TableField("`hash`")
    private String hash;

    private String nonce;

    private String transactionIndex;

    @TableField("`from`")
    private String from;
    @TableField("`to`")
    private String to;
    @TableField("`value`")
    private String value;

    private String gas;

    private String gasPrice;

    private String input;

    private String decodedInput;

    private String methodId;

    private String functionName;

    private String contractAddress;

    private String isError;

    private String cumulativeGasUsed;

    private String gasUsed;

    private String confirmations;

    private String receiptStatus;


}
