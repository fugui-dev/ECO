package com.example.eco.bean.dto;

import lombok.Data;

import java.util.Map;

@Data
public class PurchaseMinerProjectsBatchCreateResultDTO {

    private Integer successCount;

    private Integer failureCount;

    private Map<String, String> failureDetails; // key: walletAddress, value: reason
}
