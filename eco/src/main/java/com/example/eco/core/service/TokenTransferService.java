package com.example.eco.core.service;

import com.example.eco.bean.SingleResponse;
import com.example.eco.model.entity.TokenTransferLog;

import java.util.List;

/**
 * 代币转账服务接口
 */
public interface TokenTransferService {

    /**
     * 获取代币转账记录列表
     * @param tokenType 代币类型
     * @param fromAddress 发送方地址
     * @param toAddress 接收方地址
     * @param status 状态
     * @param pageNum 页码
     * @param pageSize 页大小
     * @return 转账记录列表
     */
    SingleResponse<List<TokenTransferLog>> getTokenTransferLogs(String tokenType, String fromAddress, String toAddress, String status, Integer pageNum, Integer pageSize);

    /**
     * 根据交易哈希获取转账记录
     * @param hash 交易哈希
     * @return 转账记录
     */
    SingleResponse<TokenTransferLog> getTokenTransferByHash(String hash);

    /**
     * 检查充值记录
     * @param tokenType 代币类型
     * @return 检查结果
     */
    SingleResponse<String> checkDepositRecords(String tokenType);

    /**
     * 手动同步代币转账记录
     * @param tokenType 代币类型
     * @return 同步结果
     */
    SingleResponse<String> syncTokenTransfers(String tokenType);
}
