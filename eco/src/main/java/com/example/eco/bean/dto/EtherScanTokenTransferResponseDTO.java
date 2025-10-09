package com.example.eco.bean.dto;

import lombok.Data;

import java.util.List;

/**
 * Etherscan代币转账响应DTO
 */
@Data
public class EtherScanTokenTransferResponseDTO {

    /**
     * 状态码
     */
    private String status;

    /**
     * 消息
     */
    private String message;

    /**
     * 代币转账记录列表
     */
    private List<EtherScanTokenTransferDTO> result;
}
