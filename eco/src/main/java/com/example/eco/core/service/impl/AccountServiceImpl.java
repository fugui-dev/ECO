package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.bean.dto.AccountDTO;
import com.example.eco.common.AccountTransactionStatusEnum;
import com.example.eco.common.AccountTransactionType;
import com.example.eco.common.AccountType;
import com.example.eco.core.service.AccountService;
import com.example.eco.model.entity.Account;
import com.example.eco.model.entity.AccountTransaction;
import com.example.eco.model.mapper.AccountMapper;
import com.example.eco.model.mapper.AccountTransactionMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {

    @Resource
    private AccountMapper accountMapper;
    @Resource
    private AccountTransactionMapper accountTransactionMapper;

    @Override
    public SingleResponse<Void> createAccount(AccountCreateCmd accountCreateCmd) {

        LambdaQueryWrapper<Account> ecoQueryWrapper = new LambdaQueryWrapper<>();
        ecoQueryWrapper.eq(Account::getWalletAddress, accountCreateCmd.getWalletAddress());
        ecoQueryWrapper.eq(Account::getType, AccountType.ECO.getCode());
        Account existingEcoAccount = accountMapper.selectOne(ecoQueryWrapper);
        if (existingEcoAccount != null) {
            return SingleResponse.buildFailure("账户已存在");
        }

        existingEcoAccount = new Account();
        existingEcoAccount.setWalletAddress(accountCreateCmd.getWalletAddress());
        existingEcoAccount.setSellNumber("0");
        existingEcoAccount.setSellLockNumber("0");
        existingEcoAccount.setChargeNumber("0");
        existingEcoAccount.setChargeLockNumber("0");
        existingEcoAccount.setWithdrawNumber("0");
        existingEcoAccount.setWithdrawLockNumber("0");
        existingEcoAccount.setBuyNumber("0");
        existingEcoAccount.setBuyLockNumber("0");
        existingEcoAccount.setDynamicReward("0");
        existingEcoAccount.setStaticReward("0");
        existingEcoAccount.setType(AccountType.ECO.getCode());
        existingEcoAccount.setCreateTime(System.currentTimeMillis());
        existingEcoAccount.setUpdateTime(System.currentTimeMillis());
        accountMapper.insert(existingEcoAccount);

        LambdaQueryWrapper<Account> esgQueryWrapper = new LambdaQueryWrapper<>();
        esgQueryWrapper.eq(Account::getWalletAddress, accountCreateCmd.getWalletAddress());
        esgQueryWrapper.eq(Account::getType, AccountType.ESG.getCode());
        Account existingEsgAccount = accountMapper.selectOne(ecoQueryWrapper);
        if (existingEsgAccount != null) {
            return SingleResponse.buildFailure("账户已存在");
        }

        existingEsgAccount = new Account();
        existingEsgAccount.setWalletAddress(accountCreateCmd.getWalletAddress());
        existingEsgAccount.setSellNumber("0");
        existingEsgAccount.setSellLockNumber("0");
        existingEsgAccount.setChargeNumber("0");
        existingEsgAccount.setChargeLockNumber("0");
        existingEsgAccount.setWithdrawNumber("0");
        existingEsgAccount.setWithdrawLockNumber("0");
        existingEsgAccount.setBuyNumber("0");
        existingEsgAccount.setBuyLockNumber("0");
        existingEsgAccount.setDynamicReward("0");
        existingEsgAccount.setStaticReward("0");
        existingEsgAccount.setType(AccountType.ESG.getCode());
        existingEsgAccount.setCreateTime(System.currentTimeMillis());
        existingEsgAccount.setUpdateTime(System.currentTimeMillis());
        accountMapper.insert(existingEsgAccount);

        return SingleResponse.buildSuccess();
    }

    @Override
    public MultiResponse<AccountDTO> list(AccountQry accountQry) {

        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Account::getWalletAddress, accountQry.getWalletAddress());

        List<Account> accountList = accountMapper.selectList(queryWrapper);

        List<AccountDTO> accountDTOList = new ArrayList<>();
        for (Account account : accountList) {
            AccountDTO accountDTO = new AccountDTO();
            BeanUtils.copyProperties(account, accountDTO);
            accountDTOList.add(accountDTO);
        }
        return MultiResponse.of(accountDTOList);
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> addStaticNumber(AccountStaticNumberCmd accountStaticNumberCmd) {

        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Account::getWalletAddress, accountStaticNumberCmd.getWalletAddress());
        queryWrapper.eq(Account::getType, accountStaticNumberCmd.getType());
        queryWrapper.last("FOR UPDATE");

        Account account = accountMapper.selectOne(queryWrapper);
        if (account == null) {
            return SingleResponse.buildFailure("账户不存在");
        }

        String beforeStaticReward = account.getStaticReward();

        account.setStaticReward(String.valueOf(Long.parseLong(account.getStaticReward()) + Long.parseLong(accountStaticNumberCmd.getNumber())));
        account.setUpdateTime(System.currentTimeMillis());
        int updateCount = accountMapper.update(account, queryWrapper);

        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("乐观锁异常");
        }

        AccountTransaction accountTransaction = new AccountTransaction();
        accountTransaction.setWalletAddress(accountStaticNumberCmd.getWalletAddress());
        accountTransaction.setAccountId(account.getId());
        accountTransaction.setBeforeNumber(beforeStaticReward);
        accountTransaction.setTransactionTime(System.currentTimeMillis());
        accountTransaction.setNumber(accountStaticNumberCmd.getNumber());
        accountTransaction.setAfterNumber(account.getStaticReward());
        accountTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountTransaction.setTransactionType(AccountTransactionType.STATIC_REWARD.getCode());

        accountTransactionMapper.insert(accountTransaction);

        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> addDynamicNumber(AccountDynamicNumberCmd accountDynamicNumberCmd) {
        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Account::getWalletAddress, accountDynamicNumberCmd.getWalletAddress());
        queryWrapper.eq(Account::getType, accountDynamicNumberCmd.getType());
        queryWrapper.last("FOR UPDATE");

        Account account = accountMapper.selectOne(queryWrapper);
        if (account == null) {
            return SingleResponse.buildFailure("账户不存在");
        }

        String beforeDynamicReward = account.getDynamicReward();

        account.setDynamicReward(String.valueOf(Long.parseLong(account.getDynamicReward()) + Long.parseLong(accountDynamicNumberCmd.getNumber())));
        account.setUpdateTime(System.currentTimeMillis());
        int updateCount = accountMapper.update(account, queryWrapper);

        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("乐观锁异常");
        }

        AccountTransaction accountTransaction = new AccountTransaction();
        accountTransaction.setWalletAddress(accountDynamicNumberCmd.getWalletAddress());
        accountTransaction.setAccountId(account.getId());
        accountTransaction.setBeforeNumber(beforeDynamicReward);
        accountTransaction.setTransactionTime(System.currentTimeMillis());
        accountTransaction.setNumber(accountDynamicNumberCmd.getNumber());
        accountTransaction.setAfterNumber(account.getDynamicReward());
        accountTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountTransaction.setTransactionType(AccountTransactionType.DYNAMIC_REWARD.getCode());

        accountTransactionMapper.insert(accountTransaction);

        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> buyNumber(AccountBuyNumberCmd accountBuyNumberCmd) {
        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Account::getWalletAddress, accountBuyNumberCmd.getWalletAddress());
        queryWrapper.eq(Account::getType, accountBuyNumberCmd.getType());
        queryWrapper.last("FOR UPDATE");

        Account account = accountMapper.selectOne(queryWrapper);
        if (account == null) {
            return SingleResponse.buildFailure("账户不存在");
        }

        String beforeBuyNumber = account.getBuyNumber();

        account.setBuyNumber(String.valueOf(Long.parseLong(account.getBuyNumber()) + Long.parseLong(accountBuyNumberCmd.getNumber())));
        account.setUpdateTime(System.currentTimeMillis());
        int updateCount = accountMapper.update(account, queryWrapper);

        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("乐观锁异常");
        }

        AccountTransaction accountTransaction = new AccountTransaction();
        accountTransaction.setWalletAddress(accountBuyNumberCmd.getWalletAddress());
        accountTransaction.setAccountId(account.getId());
        accountTransaction.setBeforeNumber(beforeBuyNumber);
        accountTransaction.setTransactionTime(System.currentTimeMillis());
        accountTransaction.setNumber(accountBuyNumberCmd.getNumber());
        accountTransaction.setAfterNumber(account.getBuyNumber());
        accountTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountTransaction.setTransactionType(AccountTransactionType.BUY.getCode());

        accountTransactionMapper.insert(accountTransaction);

        return SingleResponse.buildSuccess();
    }
}
