package com.example.eco.bean.dto;

import lombok.Data;

@Data
public class BscScanAccountTransactionDTO {

    private String blockNumber;

    private String blockHash;

    private String timeStamp;

    private String hash;

    private String nonce;

    private String transactionIndex;

    private String from;

    private String to;

    private String value;

    private String gas;

    private String gasPrice;

    private String input;

    private String methodId;

    private String functionName;

    private String contractAddress;

    private String isError;

    private String cumulativeGasUsed;

    private String gasUsed;

    private String confirmations;

    private String txreceipt_status;
}
