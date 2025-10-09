package com.example.eco.api.admin;

import com.example.eco.bean.SingleResponse;
import com.example.eco.core.service.TokenTransferService;
import com.example.eco.model.entity.TokenTransferLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 管理端代币转账控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/token-transfer")
public class AdminTokenTransferController {

    @Resource
    private TokenTransferService tokenTransferService;

    /**
     * 获取代币转账记录列表
     */
    @GetMapping("/list")
    public SingleResponse<List<TokenTransferLog>> getTokenTransferLogs(
            @RequestParam(required = false) String tokenType,
            @RequestParam(required = false) String fromAddress,
            @RequestParam(required = false) String toAddress,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        
        log.info("获取代币转账记录列表: tokenType={}, fromAddress={}, toAddress={}, status={}, pageNum={}, pageSize={}", 
                tokenType, fromAddress, toAddress, status, pageNum, pageSize);
        
        return tokenTransferService.getTokenTransferLogs(tokenType, fromAddress, toAddress, status, pageNum, pageSize);
    }

    /**
     * 根据交易哈希获取转账记录
     */
    @GetMapping("/{hash}")
    public SingleResponse<TokenTransferLog> getTokenTransferByHash(@PathVariable String hash) {
        log.info("根据交易哈希获取转账记录: hash={}", hash);
        return tokenTransferService.getTokenTransferByHash(hash);
    }

    /**
     * 检查充值记录
     */
    @PostMapping("/check-deposit")
    public SingleResponse<String> checkDepositRecords(@RequestParam String tokenType) {
        log.info("检查充值记录: tokenType={}", tokenType);
        return tokenTransferService.checkDepositRecords(tokenType);
    }

    /**
     * 手动同步代币转账记录
     */
    @PostMapping("/sync")
    public SingleResponse<String> syncTokenTransfers(@RequestParam String tokenType) {
        log.info("手动同步代币转账记录: tokenType={}", tokenType);
        return tokenTransferService.syncTokenTransfers(tokenType);
    }
}
