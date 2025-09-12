package com.example.eco.bean.dto;

import lombok.Data;

import java.util.List;

/**
 * BacAcan account transaction response
 */
@Data
public class BscScanAccountTransactionResponseDTO {

    private String status;

    private String message;

    private List<BscScanAccountTransactionDTO> result;
}
