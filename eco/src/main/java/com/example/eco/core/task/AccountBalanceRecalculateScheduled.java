package com.example.eco.core.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eco.bean.SingleResponse;
import com.example.eco.common.AccountType;
import com.example.eco.model.entity.Account;
import com.example.eco.model.entity.AccountTransaction;
import com.example.eco.model.mapper.AccountMapper;
import com.example.eco.model.mapper.AccountTransactionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class   AccountBalanceRecalculateScheduled {

    @Resource
    private AccountMapper accountMapper;
    
    @Resource
    private AccountTransactionMapper accountTransactionMapper;

    /**
     * 重新计算账户余额
     * 根据指定时间之前的流水重新计算所有账户的余额
     * @param beforeTime 时间戳，重放此时间之前的所有流水
     * @return 执行结果
     */
    @Transactional(rollbackFor = Exception.class)
    public SingleResponse<String> recalculateAccountBalance(Long beforeTime) {
        log.info("开始重新计算账户余额，重放时间: {}", beforeTime);
        
        try {
            // 1. 获取所有账户
            List<Account> allAccounts = accountMapper.selectList(new LambdaQueryWrapper<>());
            log.info("找到{}个账户需要重新计算", allAccounts.size());
            
            // 2. 获取指定时间之前的所有流水，按钱包地址和账户类型分组
            LambdaQueryWrapper<AccountTransaction> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.lt(AccountTransaction::getTransactionTime, beforeTime);
            queryWrapper.orderByAsc(AccountTransaction::getTransactionTime);
            
            List<AccountTransaction> allTransactions = accountTransactionMapper.selectList(queryWrapper);
            log.info("找到{}条流水记录需要重放", allTransactions.size());
            
            // 按钱包地址和账户类型分组
            Map<String, List<AccountTransaction>> transactionsByAccount = allTransactions.stream()
                    .filter(t -> t.getWalletAddress() != null && t.getAccountType() != null)
                    .collect(Collectors.groupingBy(t -> t.getWalletAddress() + "_" + t.getAccountType()));
            
            int processedAccounts = 0;
            int processedTransactions = 0;
            
            // 3. 为每个账户重新计算余额
            for (Account account : allAccounts) {
                String accountKey = account.getWalletAddress() + "_" + account.getType();
                List<AccountTransaction> accountTransactions = transactionsByAccount.get(accountKey);
                
                if (accountTransactions == null || accountTransactions.isEmpty()) {
                    log.debug("账户{}没有流水记录", accountKey);
                    continue;
                }
                
                // 重置账户余额为0
                AccountRecalculateResult result = resetAccountBalance(account);
                
                // 重放流水
                for (AccountTransaction transaction : accountTransactions) {
                    processTransaction(account, transaction, result);
                    processedTransactions++;
                }
                
                // 更新账户余额
                updateAccountBalance(account, result);
                processedAccounts++;
                
                log.debug("账户{}重放完成，最终余额: {}, 静态奖励: {}, 动态奖励: {}", 
                    accountKey, account.getNumber(), account.getStaticReward(), account.getDynamicReward());
            }
            
            String resultMessage = String.format("账户余额重放完成，处理了%d个账户，重放了%d条流水记录", 
                    processedAccounts, processedTransactions);
            log.info(resultMessage);
            return SingleResponse.of(resultMessage);
            
        } catch (Exception e) {
            log.error("重新计算账户余额异常", e);
            return SingleResponse.buildFailure("重新计算账户余额异常: " + e.getMessage());
        }
    }
    
    /**
     * 重置账户余额为初始状态
     */
    private AccountRecalculateResult resetAccountBalance(Account account) {
        AccountRecalculateResult result = new AccountRecalculateResult();
        
        // 重置所有余额字段为0
        result.setNumber(BigDecimal.ZERO);
        result.setStaticReward(BigDecimal.ZERO);
        result.setDynamicReward(BigDecimal.ZERO);
        result.setBuyNumber(BigDecimal.ZERO);
        result.setSellNumber(BigDecimal.ZERO);
        result.setChargeNumber(BigDecimal.ZERO);
        result.setWithdrawNumber(BigDecimal.ZERO);
        result.setServiceNumber(BigDecimal.ZERO);
        result.setBuyLockNumber(BigDecimal.ZERO);
        result.setSellLockNumber(BigDecimal.ZERO);
        result.setChargeLockNumber(BigDecimal.ZERO);
        result.setWithdrawLockNumber(BigDecimal.ZERO);
        result.setServiceLockNumber(BigDecimal.ZERO);
        
        return result;
    }
    
    /**
     * 处理单条流水记录
     */
    private void processTransaction(Account account, AccountTransaction transaction, AccountRecalculateResult result) {
        String transactionType = transaction.getTransactionType();
        String numberStr = transaction.getNumber();
        
        // 检查必要字段
        if (transactionType == null || numberStr == null || numberStr.trim().isEmpty()) {
            log.warn("流水记录字段不完整，跳过处理: transactionType={}, number={}", transactionType, numberStr);
            return;
        }
        
        BigDecimal amount;
        try {
            amount = new BigDecimal(numberStr);
        } catch (NumberFormatException e) {
            log.warn("流水记录数量格式错误，跳过处理: number={}", numberStr);
            return;
        }
        
        switch (transactionType) {
            case "ADD_NUMBER":
                // 增加可用积分
                result.setNumber(result.getNumber().add(amount));
                break;
                
            case "DEDUCT_NUMBER":
                // 扣除可用积分
                result.setNumber(result.getNumber().subtract(amount));
                break;
                
            case "STATIC_REWARD":
                // 静态奖励
                result.setStaticReward(result.getStaticReward().add(amount));
                break;
                
            case "DEDUCT_STATIC_REWARD":
                // 扣除静态奖励
                result.setStaticReward(result.getStaticReward().subtract(amount));
                break;
                
            case "DYNAMIC_REWARD":
                // 动态奖励
                result.setDynamicReward(result.getDynamicReward().add(amount));
                break;
                
            case "DEDUCT_DYNAMIC_REWARD":
                // 扣除动态奖励
                result.setDynamicReward(result.getDynamicReward().subtract(amount));
                break;
                
            case "BUY":
                // 买入
                result.setBuyNumber(result.getBuyNumber().add(amount));
                break;
                
            case "LOCK_BUY":
                // 锁定买入
                result.setBuyLockNumber(result.getBuyLockNumber().add(amount));
                break;
                
            case "RELEASE_LOCK_BUY":
                // 释放锁定买入
                result.setBuyLockNumber(result.getBuyLockNumber().subtract(amount));
                break;
                
            case "ROLLBACK_LOCK_BUY":
                // 回滚锁定买入
                result.setBuyLockNumber(result.getBuyLockNumber().subtract(amount));
                break;
                
            case "SELL":
                // 卖出
                result.setSellNumber(result.getSellNumber().add(amount));
                break;
                
            case "LOCK_SELL":
                // 锁定卖出
                result.setSellLockNumber(result.getSellLockNumber().add(amount));
                break;
                
            case "RELEASE_LOCK_SELL":
                // 释放锁定卖出
                result.setSellLockNumber(result.getSellLockNumber().subtract(amount));
                break;
                
            case "ROLLBACK_LOCK_SELL":
                // 回滚锁定卖出
                result.setSellLockNumber(result.getSellLockNumber().subtract(amount));
                break;
                
            case "CHARGE":
                // 充值
                result.setChargeNumber(result.getChargeNumber().add(amount));
                break;
                
            case "LOCK_CHARGE":
                // 锁定充值
                result.setChargeLockNumber(result.getChargeLockNumber().add(amount));
                break;
                
            case "RELEASE_LOCK_CHARGE":
                // 释放锁定充值
                result.setChargeLockNumber(result.getChargeLockNumber().subtract(amount));
                break;
                
            case "ROLLBACK_LOCK_CHARGE":
                // 回滚锁定充值
                result.setChargeLockNumber(result.getChargeLockNumber().subtract(amount));
                break;
                
            case "WITHDRAW":
                // 提现
                result.setWithdrawNumber(result.getWithdrawNumber().add(amount));
                break;
                
            case "LOCK_WITHDRAW":
                // 锁定提现
                result.setWithdrawLockNumber(result.getWithdrawLockNumber().add(amount));
                break;
                
            case "RELEASE_LOCK_WITHDRAW":
                // 释放锁定提现
                result.setWithdrawLockNumber(result.getWithdrawLockNumber().subtract(amount));
                break;
                
            case "ROLLBACK_LOCK_WITHDRAW":
                // 回滚锁定提现
                result.setWithdrawLockNumber(result.getWithdrawLockNumber().subtract(amount));
                break;
                
            case "WITHDRAW_SERVICE":
                // 提现服务费
                result.setServiceNumber(result.getServiceNumber().add(amount));
                break;
                
            case "LOCK_WITHDRAW_SERVICE":
                // 锁定提现服务费
                result.setServiceLockNumber(result.getServiceLockNumber().add(amount));
                break;
                
            case "RELEASE_LOCK_WITHDRAW_SERVICE":
                // 释放锁定提现服务费
                result.setServiceLockNumber(result.getServiceLockNumber().subtract(amount));
                break;
                
            case "ROLLBACK_LOCK_WITHDRAW_SERVICE":
                // 回滚锁定提现服务费
                result.setServiceLockNumber(result.getServiceLockNumber().subtract(amount));
                break;
                
            case "DEDUCT_CHARGE":
                // 扣除充值
                result.setChargeNumber(result.getChargeNumber().subtract(amount));
                break;
                
            default:
                log.warn("未知的交易类型: {}", transactionType);
                break;
        }
    }
    
    /**
     * 更新账户余额
     */
    private void updateAccountBalance(Account account, AccountRecalculateResult result) {
        // 确保余额不为负数
        account.setNumber(result.getNumber().max(BigDecimal.ZERO).toString());
        account.setStaticReward(result.getStaticReward().max(BigDecimal.ZERO).toString());
        account.setDynamicReward(result.getDynamicReward().max(BigDecimal.ZERO).toString());
        account.setBuyNumber(result.getBuyNumber().max(BigDecimal.ZERO).toString());
        account.setSellNumber(result.getSellNumber().max(BigDecimal.ZERO).toString());
        account.setChargeNumber(result.getChargeNumber().max(BigDecimal.ZERO).toString());
        account.setWithdrawNumber(result.getWithdrawNumber().max(BigDecimal.ZERO).toString());
        account.setServiceNumber(result.getServiceNumber().max(BigDecimal.ZERO).toString());
        account.setBuyLockNumber(result.getBuyLockNumber().max(BigDecimal.ZERO).toString());
        account.setSellLockNumber(result.getSellLockNumber().max(BigDecimal.ZERO).toString());
        account.setChargeLockNumber(result.getChargeLockNumber().max(BigDecimal.ZERO).toString());
        account.setWithdrawLockNumber(result.getWithdrawLockNumber().max(BigDecimal.ZERO).toString());
        account.setServiceLockNumber(result.getServiceLockNumber().max(BigDecimal.ZERO).toString());
        account.setUpdateTime(System.currentTimeMillis());
        
        accountMapper.updateById(account);
    }
    
    /**
     * 账户重算结果类
     */
    private static class AccountRecalculateResult {
        private BigDecimal number = BigDecimal.ZERO;
        private BigDecimal staticReward = BigDecimal.ZERO;
        private BigDecimal dynamicReward = BigDecimal.ZERO;
        private BigDecimal buyNumber = BigDecimal.ZERO;
        private BigDecimal sellNumber = BigDecimal.ZERO;
        private BigDecimal chargeNumber = BigDecimal.ZERO;
        private BigDecimal withdrawNumber = BigDecimal.ZERO;
        private BigDecimal serviceNumber = BigDecimal.ZERO;
        private BigDecimal buyLockNumber = BigDecimal.ZERO;
        private BigDecimal sellLockNumber = BigDecimal.ZERO;
        private BigDecimal chargeLockNumber = BigDecimal.ZERO;
        private BigDecimal withdrawLockNumber = BigDecimal.ZERO;
        private BigDecimal serviceLockNumber = BigDecimal.ZERO;
        
        // Getters and Setters
        public BigDecimal getNumber() { return number; }
        public void setNumber(BigDecimal number) { this.number = number; }
        
        public BigDecimal getStaticReward() { return staticReward; }
        public void setStaticReward(BigDecimal staticReward) { this.staticReward = staticReward; }
        
        public BigDecimal getDynamicReward() { return dynamicReward; }
        public void setDynamicReward(BigDecimal dynamicReward) { this.dynamicReward = dynamicReward; }
        
        public BigDecimal getBuyNumber() { return buyNumber; }
        public void setBuyNumber(BigDecimal buyNumber) { this.buyNumber = buyNumber; }
        
        public BigDecimal getSellNumber() { return sellNumber; }
        public void setSellNumber(BigDecimal sellNumber) { this.sellNumber = sellNumber; }
        
        public BigDecimal getChargeNumber() { return chargeNumber; }
        public void setChargeNumber(BigDecimal chargeNumber) { this.chargeNumber = chargeNumber; }
        
        public BigDecimal getWithdrawNumber() { return withdrawNumber; }
        public void setWithdrawNumber(BigDecimal withdrawNumber) { this.withdrawNumber = withdrawNumber; }
        
        public BigDecimal getServiceNumber() { return serviceNumber; }
        public void setServiceNumber(BigDecimal serviceNumber) { this.serviceNumber = serviceNumber; }
        
        public BigDecimal getBuyLockNumber() { return buyLockNumber; }
        public void setBuyLockNumber(BigDecimal buyLockNumber) { this.buyLockNumber = buyLockNumber; }
        
        public BigDecimal getSellLockNumber() { return sellLockNumber; }
        public void setSellLockNumber(BigDecimal sellLockNumber) { this.sellLockNumber = sellLockNumber; }
        
        public BigDecimal getChargeLockNumber() { return chargeLockNumber; }
        public void setChargeLockNumber(BigDecimal chargeLockNumber) { this.chargeLockNumber = chargeLockNumber; }
        
        public BigDecimal getWithdrawLockNumber() { return withdrawLockNumber; }
        public void setWithdrawLockNumber(BigDecimal withdrawLockNumber) { this.withdrawLockNumber = withdrawLockNumber; }
        
        public BigDecimal getServiceLockNumber() { return serviceLockNumber; }
        public void setServiceLockNumber(BigDecimal serviceLockNumber) { this.serviceLockNumber = serviceLockNumber; }
    }
}