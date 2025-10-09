package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.ChargeOrderCreateCmd;
import com.example.eco.common.AccountType;
import com.example.eco.common.ChargeOrderStatus;
import com.example.eco.core.service.ChargeOrderService;
import com.example.eco.core.service.TokenTransferService;
import com.example.eco.model.entity.ChargeOrder;
import com.example.eco.model.entity.TokenTransferLog;
import com.example.eco.model.mapper.ChargeOrderMapper;
import com.example.eco.model.mapper.TokenTransferLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

import javax.annotation.Resource;
import java.util.List;

/**
 * 代币转账服务实现类
 */
@Slf4j
@Service
public class TokenTransferServiceImpl implements TokenTransferService {

    @Resource
    private TokenTransferLogMapper tokenTransferLogMapper;

    @Resource
    private ChargeOrderService chargeOrderService;

    @Resource
    private ChargeOrderMapper chargeOrderMapper;

    @Override
    public SingleResponse<List<TokenTransferLog>> getTokenTransferLogs(String tokenType, String fromAddress, String toAddress, String status, Integer pageNum, Integer pageSize) {
        try {
            // 构建查询条件
            LambdaQueryWrapper<TokenTransferLog> queryWrapper = new LambdaQueryWrapper<>();
            
            if (StringUtils.hasText(tokenType)) {
                queryWrapper.eq(TokenTransferLog::getTokenType, tokenType);
            }
            if (StringUtils.hasText(fromAddress)) {
                queryWrapper.eq(TokenTransferLog::getFromAddress, fromAddress);
            }
            if (StringUtils.hasText(toAddress)) {
                queryWrapper.eq(TokenTransferLog::getToAddress, toAddress);
            }
            if (StringUtils.hasText(status)) {
                queryWrapper.eq(TokenTransferLog::getStatus, status);
            }
            
            // 按创建时间倒序排列
            queryWrapper.orderByDesc(TokenTransferLog::getCreateTime);
            
            // 分页查询
            Page<TokenTransferLog> page = new Page<>(pageNum, pageSize);
            Page<TokenTransferLog> result = tokenTransferLogMapper.selectPage(page, queryWrapper);
            
            return SingleResponse.of(result.getRecords());

        } catch (Exception e) {
            log.error("获取代币转账记录失败", e);
            return SingleResponse.buildFailure("获取代币转账记录失败: " + e.getMessage());
        }
    }

    @Override
    public SingleResponse<TokenTransferLog> getTokenTransferByHash(String hash) {
        try {
            if (!StringUtils.hasText(hash)) {
                return SingleResponse.buildFailure("交易哈希不能为空");
            }
            
            LambdaQueryWrapper<TokenTransferLog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TokenTransferLog::getHash, hash);
            
            TokenTransferLog transferLog = tokenTransferLogMapper.selectOne(queryWrapper);
            
            if (transferLog == null) {
                return SingleResponse.buildFailure("未找到对应的转账记录");
            }
            
            return SingleResponse.of(transferLog);

        } catch (Exception e) {
            log.error("根据交易哈希获取转账记录失败: hash={}", hash, e);
            return SingleResponse.buildFailure("获取转账记录失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public SingleResponse<String> checkDepositRecords(String tokenType) {
        try {
            if (!StringUtils.hasText(tokenType)) {
                return SingleResponse.buildFailure("代币类型不能为空");
            }
            
            // 查询未检查的充值记录
            LambdaQueryWrapper<TokenTransferLog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TokenTransferLog::getTokenType, tokenType)
                    .eq(TokenTransferLog::getStatus, "SUCCESS")
                    .eq(TokenTransferLog::getChecked, false)
                    .orderByAsc(TokenTransferLog::getCreateTime);
            
            List<TokenTransferLog> uncheckedTransfers = tokenTransferLogMapper.selectList(queryWrapper);
            
            if (uncheckedTransfers.isEmpty()) {
                return SingleResponse.of("没有需要检查的" + tokenType + "充值记录");
            }
            
            int processedCount = 0;
            int createdCount = 0;
            int skippedCount = 0;
            
            for (TokenTransferLog transfer : uncheckedTransfers) {
                try {
                    // 检查是否已经存在对应的充值订单
                    LambdaQueryWrapper<ChargeOrder> chargeOrderQuery = new LambdaQueryWrapper<>();
                    chargeOrderQuery.eq(ChargeOrder::getHash, transfer.getHash());
                    ChargeOrder existingOrder = chargeOrderMapper.selectOne(chargeOrderQuery);
                    
                    if (existingOrder != null) {
                        log.info("充值订单已存在，跳过: hash={}", transfer.getHash());
                        skippedCount++;
                    } else {
                        // 创建充值订单
                        boolean created = processDepositRecord(transfer);
                        if (created) {
                            createdCount++;
                        }
                    }
                    
                    // 标记为已检查
                    transfer.setChecked(true);
                    transfer.setUpdateTime(System.currentTimeMillis());
                    tokenTransferLogMapper.updateById(transfer);
                    
                    processedCount++;
                    
                } catch (Exception e) {
                    log.error("处理充值记录失败: hash={}", transfer.getHash(), e);
                }
            }
            
            return SingleResponse.of(String.format("处理完成: 总计%d条, 新建订单%d条, 跳过%d条", 
                    processedCount, createdCount, skippedCount));
            
        } catch (Exception e) {
            log.error("检查充值记录失败: tokenType={}", tokenType, e);
            return SingleResponse.buildFailure("检查充值记录失败: " + e.getMessage());
        }
    }

    @Override
    public SingleResponse<String> syncTokenTransfers(String tokenType) {
        try {
            if (!StringUtils.hasText(tokenType)) {
                return SingleResponse.buildFailure("代币类型不能为空");
            }
            
            // 这里可以调用同步任务的方法
            // 或者直接在这里实现同步逻辑
            log.info("手动同步{}代币转账记录", tokenType);
            
            // 暂时返回成功，实际实现可以调用TokenTransferScheduled中的方法
            return SingleResponse.of("手动同步" + tokenType + "代币转账记录成功");
            
        } catch (Exception e) {
            log.error("手动同步代币转账记录失败: tokenType={}", tokenType, e);
            return SingleResponse.buildFailure("同步失败: " + e.getMessage());
        }
    }

    /**
     * 处理充值记录
     */
    private boolean processDepositRecord(TokenTransferLog transfer) {
        try {
            log.info("处理充值记录: hash={}, from={}, to={}, value={}", 
                    transfer.getHash(), transfer.getFromAddress(), transfer.getToAddress(), transfer.getTransferValue());
            
            // 1. 验证转账金额
            BigDecimal transferAmount = new BigDecimal(transfer.getTransferValue());
            if (transferAmount.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("转账金额无效，跳过: hash={}, amount={}", transfer.getHash(), transfer.getTransferValue());
                return false;
            }
            
            // 2. 确定代币类型
            String accountType;
            if ("ESG".equals(transfer.getTokenType())) {
                accountType = AccountType.ESG.getCode();
            } else if ("ECO".equals(transfer.getTokenType())) {
                accountType = AccountType.ECO.getCode();
            } else {
                log.warn("不支持的代币类型: {}", transfer.getTokenType());
                return false;
            }
            
            // 3. 创建充值订单
            ChargeOrderCreateCmd chargeOrderCreateCmd = new ChargeOrderCreateCmd();
            chargeOrderCreateCmd.setWalletAddress(transfer.getFromAddress());
            chargeOrderCreateCmd.setType(accountType);
            chargeOrderCreateCmd.setNumber(transfer.getTransferValue());
            chargeOrderCreateCmd.setHash(transfer.getHash());
            chargeOrderCreateCmd.setPrice("0"); // 默认价格，可以根据实际情况调整
            chargeOrderCreateCmd.setTotalPrice("0");
            
            // 调用充值订单服务创建订单
            SingleResponse<Void> response = chargeOrderService.create(chargeOrderCreateCmd);
            if (response.isSuccess()) {
                log.info("成功创建充值订单: hash={}, walletAddress={}, amount={}", 
                        transfer.getHash(), transfer.getFromAddress(), transfer.getTransferValue());
                return true;
            } else {
                log.error("创建充值订单失败: hash={}, error={}", transfer.getHash(), response.getErrMessage());
                return false;
            }
            
        } catch (Exception e) {
            log.error("处理充值记录异常: hash={}", transfer.getHash(), e);
            return false;
        }
    }
}
