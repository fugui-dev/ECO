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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

        if (Objects.isNull(existingEcoAccount)) {

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
        }


        LambdaQueryWrapper<Account> esgQueryWrapper = new LambdaQueryWrapper<>();
        esgQueryWrapper.eq(Account::getWalletAddress, accountCreateCmd.getWalletAddress());
        esgQueryWrapper.eq(Account::getType, AccountType.ESG.getCode());
        Account existingEsgAccount = accountMapper.selectOne(ecoQueryWrapper);
        if (Objects.isNull(existingEsgAccount)) {
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
        }


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

        String beforeNumber = account.getNumber();

        String beforeStaticReward = account.getStaticReward();

        account.setNumber(String.valueOf(new BigDecimal(account.getNumber()).add(new BigDecimal(accountStaticNumberCmd.getNumber()))));
        account.setStaticReward(String.valueOf(new BigDecimal(account.getStaticReward()).add(new BigDecimal(accountStaticNumberCmd.getNumber()))));
        account.setUpdateTime(System.currentTimeMillis());
        int updateCount = accountMapper.updateById(account);

        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("乐观锁异常");
        }

        AccountTransaction accountTransaction = new AccountTransaction();
        accountTransaction.setWalletAddress(accountStaticNumberCmd.getWalletAddress());
        accountTransaction.setAccountId(account.getId());
        accountTransaction.setBeforeNumber(beforeNumber);
        accountTransaction.setTransactionTime(System.currentTimeMillis());
        accountTransaction.setNumber(accountStaticNumberCmd.getNumber());
        accountTransaction.setAfterNumber(account.getNumber());
        accountTransaction.setAccountType(account.getType());
        accountTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountTransaction.setTransactionType(AccountTransactionType.ADD_NUMBER.getCode());
        accountTransactionMapper.insert(accountTransaction);

        AccountTransaction accountStaticTransaction = new AccountTransaction();
        accountStaticTransaction.setWalletAddress(accountStaticNumberCmd.getWalletAddress());
        accountStaticTransaction.setAccountId(account.getId());
        accountStaticTransaction.setBeforeNumber(beforeStaticReward);
        accountStaticTransaction.setTransactionTime(System.currentTimeMillis());
        accountStaticTransaction.setNumber(accountStaticNumberCmd.getNumber());
        accountStaticTransaction.setAfterNumber(account.getStaticReward());
        accountStaticTransaction.setAccountType(account.getType());
        accountStaticTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountStaticTransaction.setTransactionType(AccountTransactionType.STATIC_REWARD.getCode());

        accountTransactionMapper.insert(accountStaticTransaction);

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

        String beforeNumber = account.getNumber();

        String beforeDynamicReward = account.getDynamicReward();

        account.setNumber(String.valueOf(new BigDecimal(account.getNumber()).add(new BigDecimal(accountDynamicNumberCmd.getNumber()))));
        account.setDynamicReward(String.valueOf(new BigDecimal(account.getDynamicReward()).add(new BigDecimal(accountDynamicNumberCmd.getNumber()))));
        account.setUpdateTime(System.currentTimeMillis());
        int updateCount = accountMapper.updateById(account);

        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("乐观锁异常");
        }

        AccountTransaction accountTransaction = new AccountTransaction();
        accountTransaction.setWalletAddress(accountDynamicNumberCmd.getWalletAddress());
        accountTransaction.setAccountId(account.getId());
        accountTransaction.setBeforeNumber(beforeNumber);
        accountTransaction.setTransactionTime(System.currentTimeMillis());
        accountTransaction.setNumber(accountDynamicNumberCmd.getNumber());
        accountTransaction.setAfterNumber(account.getNumber());
        accountTransaction.setAccountType(account.getType());
        accountTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountTransaction.setTransactionType(AccountTransactionType.ADD_NUMBER.getCode());

        accountTransactionMapper.insert(accountTransaction);

        AccountTransaction accountDynamicTransaction = new AccountTransaction();
        accountDynamicTransaction.setWalletAddress(accountDynamicNumberCmd.getWalletAddress());
        accountDynamicTransaction.setAccountId(account.getId());
        accountDynamicTransaction.setBeforeNumber(beforeDynamicReward);
        accountDynamicTransaction.setTransactionTime(System.currentTimeMillis());
        accountDynamicTransaction.setNumber(accountDynamicNumberCmd.getNumber());
        accountDynamicTransaction.setAfterNumber(account.getDynamicReward());
        accountDynamicTransaction.setAccountType(account.getType());
        accountDynamicTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountDynamicTransaction.setTransactionType(AccountTransactionType.DYNAMIC_REWARD.getCode());

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

        String beforeBuyLockNumber = account.getBuyLockNumber();
        account.setBuyLockNumber(String.valueOf(new BigDecimal(account.getBuyLockNumber()).add(new BigDecimal(accountBuyNumberCmd.getNumber())))));
        account.setUpdateTime(System.currentTimeMillis());

        int updateCount = accountMapper.updateById(account);

        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("乐观锁异常");
        }

        AccountTransaction accountLockBuyTransaction = new AccountTransaction();
        accountLockBuyTransaction.setWalletAddress(accountBuyNumberCmd.getWalletAddress());
        accountLockBuyTransaction.setAccountId(account.getId());
        accountLockBuyTransaction.setBeforeNumber(beforeBuyLockNumber);
        accountLockBuyTransaction.setTransactionTime(System.currentTimeMillis());
        accountLockBuyTransaction.setNumber(accountBuyNumberCmd.getNumber());
        accountLockBuyTransaction.setAfterNumber(account.getBuyLockNumber());
        accountLockBuyTransaction.setAccountType(account.getType());
        accountLockBuyTransaction.setStatus(AccountTransactionStatusEnum.DEALING.getCode());
        accountLockBuyTransaction.setTransactionType(AccountTransactionType.LOCK_BUY.getCode());
        accountLockBuyTransaction.setOrder(accountBuyNumberCmd.getOrder());

        accountTransactionMapper.insert(accountLockBuyTransaction);

        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> releaseLockBuyNumber(AccountReleaseLockBuyNumberCmd accountReleaseLockBuyNumberCmd) {
        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Account::getWalletAddress, accountReleaseLockBuyNumberCmd.getWalletAddress());
        queryWrapper.eq(Account::getType, accountReleaseLockBuyNumberCmd.getType());
        queryWrapper.last("FOR UPDATE");

        Account account = accountMapper.selectOne(queryWrapper);
        if (account == null) {
            return SingleResponse.buildFailure("账户不存在");
        }

        String beforeNumber = account.getNumber();

        String beforeLockBuyNumber = account.getBuyLockNumber();

        account.setNumber(String.valueOf(new BigDecimal(account.getNumber()).add(new BigDecimal(accountReleaseLockBuyNumberCmd.getNumber()))));
        account.setBuyLockNumber(String.valueOf(new BigDecimal(account.getBuyLockNumber()).subtract(new BigDecimal(accountReleaseLockBuyNumberCmd.getNumber())))));
        account.setUpdateTime(System.currentTimeMillis());
        int updateCount = accountMapper.updateById(account);

        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("乐观锁异常");
        }

        AccountTransaction accountTransaction = new AccountTransaction();
        accountTransaction.setWalletAddress(accountReleaseLockBuyNumberCmd.getWalletAddress());
        accountTransaction.setAccountId(account.getId());
        accountTransaction.setBeforeNumber(beforeNumber);
        accountTransaction.setTransactionTime(System.currentTimeMillis());
        accountTransaction.setNumber(accountReleaseLockBuyNumberCmd.getNumber());
        accountTransaction.setAfterNumber(account.getNumber());
        accountTransaction.setAccountType(account.getType());
        accountTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountTransaction.setTransactionType(AccountTransactionType.ADD_NUMBER.getCode());
        accountTransaction.setOrder(accountReleaseLockBuyNumberCmd.getOrder());

        accountTransactionMapper.insert(accountTransaction);

        AccountTransaction accountReleaseBuyTransaction = new AccountTransaction();
        accountReleaseBuyTransaction.setWalletAddress(accountReleaseLockBuyNumberCmd.getWalletAddress());
        accountReleaseBuyTransaction.setAccountId(account.getId());
        accountReleaseBuyTransaction.setBeforeNumber(beforeLockBuyNumber);
        accountReleaseBuyTransaction.setTransactionTime(System.currentTimeMillis());
        accountReleaseBuyTransaction.setNumber(accountReleaseLockBuyNumberCmd.getNumber());
        accountReleaseBuyTransaction.setAfterNumber(account.getBuyLockNumber());
        accountReleaseBuyTransaction.setAccountType(account.getType());
        accountReleaseBuyTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountReleaseBuyTransaction.setTransactionType(AccountTransactionType.RELEASE_LOCK_BUY.getCode());
        accountReleaseBuyTransaction.setOrder(accountReleaseLockBuyNumberCmd.getOrder());

        accountTransactionMapper.insert(accountReleaseBuyTransaction);

        LambdaQueryWrapper<AccountTransaction> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(AccountTransaction::getOrder, accountReleaseLockBuyNumberCmd.getOrder());
        lambdaQueryWrapper.eq(AccountTransaction::getTransactionType, AccountTransactionType.LOCK_BUY.getCode());
        lambdaQueryWrapper.eq(AccountTransaction::getStatus, AccountTransactionStatusEnum.DEALING.getCode());

        AccountTransaction lockBuyTransaction = accountTransactionMapper.selectOne(lambdaQueryWrapper);
        if (Objects.nonNull(lockBuyTransaction)) {
            lockBuyTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
            accountTransactionMapper.updateById(lockBuyTransaction);
        }

        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> sellNumber(AccountSellNumberCmd accountSellNumberCmd) {
        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Account::getWalletAddress, accountSellNumberCmd.getWalletAddress());
        queryWrapper.eq(Account::getType, accountSellNumberCmd.getType());
        queryWrapper.last("FOR UPDATE");

        Account account = accountMapper.selectOne(queryWrapper);
        if (account == null) {
            return SingleResponse.buildFailure("账户不存在");
        }

        String beforeSellNumber = account.getSellNumber();
        String beforeSellLockNumber = account.getSellLockNumber();


        account.setSellNumber(String.valueOf(Long.parseLong(account.getSellNumber()) + Long.parseLong(accountSellNumberCmd.getNumber())));
        account.setSellLockNumber(String.valueOf(Long.parseLong(account.getSellLockNumber()) - Long.parseLong(accountSellNumberCmd.getNumber())));
        account.setUpdateTime(System.currentTimeMillis());
        int updateCount = accountMapper.updateById(account);

        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("乐观锁异常");
        }

        AccountTransaction accountSellTransaction = new AccountTransaction();
        accountSellTransaction.setWalletAddress(accountSellNumberCmd.getWalletAddress());
        accountSellTransaction.setAccountId(account.getId());
        accountSellTransaction.setBeforeNumber(beforeSellNumber);
        accountSellTransaction.setTransactionTime(System.currentTimeMillis());
        accountSellTransaction.setNumber(accountSellNumberCmd.getNumber());
        accountSellTransaction.setAfterNumber(account.getSellNumber());
        accountSellTransaction.setAccountType(account.getType());
        accountSellTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountSellTransaction.setTransactionType(AccountTransactionType.SELL.getCode());

        accountTransactionMapper.insert(accountSellTransaction);

        AccountTransaction accountLockSellTransaction = new AccountTransaction();
        accountLockSellTransaction.setWalletAddress(accountSellNumberCmd.getWalletAddress());
        accountLockSellTransaction.setAccountId(account.getId());
        accountLockSellTransaction.setBeforeNumber(beforeSellLockNumber);
        accountLockSellTransaction.setTransactionTime(System.currentTimeMillis());
        accountLockSellTransaction.setNumber(accountSellNumberCmd.getNumber());
        accountLockSellTransaction.setAfterNumber(account.getSellLockNumber());
        accountLockSellTransaction.setAccountType(account.getType());
        accountLockSellTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountLockSellTransaction.setTransactionType(AccountTransactionType.RELEASE_LOCK_SELL.getCode());

        accountTransactionMapper.insert(accountSellTransaction);

        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> lockSellNumber(AccountLockSellNumberCmd accountLockSellNumberCmd) {
        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Account::getWalletAddress, accountLockSellNumberCmd.getWalletAddress());
        queryWrapper.eq(Account::getType, accountLockSellNumberCmd.getType());
        queryWrapper.last("FOR UPDATE");

        Account account = accountMapper.selectOne(queryWrapper);
        if (account == null) {
            return SingleResponse.buildFailure("账户不存在");
        }

        String beforeLockSellNumber = account.getSellLockNumber();

        account.setSellLockNumber(String.valueOf(Long.parseLong(account.getSellLockNumber()) + Long.parseLong(accountLockSellNumberCmd.getNumber())));
        account.setUpdateTime(System.currentTimeMillis());
        int updateCount = accountMapper.updateById(account);

        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("乐观锁异常");
        }

        AccountTransaction accountTransaction = new AccountTransaction();
        accountTransaction.setWalletAddress(accountLockSellNumberCmd.getWalletAddress());
        accountTransaction.setAccountId(account.getId());
        accountTransaction.setBeforeNumber(beforeLockSellNumber);
        accountTransaction.setTransactionTime(System.currentTimeMillis());
        accountTransaction.setNumber(accountLockSellNumberCmd.getNumber());
        accountTransaction.setAfterNumber(account.getSellLockNumber());
        accountTransaction.setAccountType(account.getType());
        accountTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountTransaction.setTransactionType(AccountTransactionType.LOCK_SELL.getCode());

        accountTransactionMapper.insert(accountTransaction);

        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> chargeNumber(AccountChargeNumberCmd accountChargeNumberCmd) {
        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Account::getWalletAddress, accountChargeNumberCmd.getWalletAddress());
        queryWrapper.eq(Account::getType, accountChargeNumberCmd.getType());
        queryWrapper.last("FOR UPDATE");

        Account account = accountMapper.selectOne(queryWrapper);
        if (account == null) {
            return SingleResponse.buildFailure("账户不存在");
        }

        String beforeChargeNumber = account.getChargeNumber();
        String beforeChargeLockNumber = account.getChargeLockNumber();

        account.setChargeNumber(String.valueOf(Long.parseLong(account.getChargeNumber()) + Long.parseLong(accountChargeNumberCmd.getNumber())));
        account.setChargeLockNumber(String.valueOf(Long.parseLong(account.getChargeLockNumber()) - Long.parseLong(accountChargeNumberCmd.getNumber())));

        account.setUpdateTime(System.currentTimeMillis());
        int updateCount = accountMapper.updateById(account);

        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("乐观锁异常");
        }

        AccountTransaction accountChargeTransaction = new AccountTransaction();
        accountChargeTransaction.setWalletAddress(accountChargeNumberCmd.getWalletAddress());
        accountChargeTransaction.setAccountId(account.getId());
        accountChargeTransaction.setBeforeNumber(beforeChargeNumber);
        accountChargeTransaction.setTransactionTime(System.currentTimeMillis());
        accountChargeTransaction.setNumber(accountChargeNumberCmd.getNumber());
        accountChargeTransaction.setAfterNumber(account.getChargeNumber());
        accountChargeTransaction.setAccountType(account.getType());
        accountChargeTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountChargeTransaction.setTransactionType(AccountTransactionType.CHARGE.getCode());
        accountChargeTransaction.setHash(accountChargeNumberCmd.getHash());
        accountTransactionMapper.insert(accountChargeTransaction);

        AccountTransaction accountLockChargeTransaction = new AccountTransaction();
        accountLockChargeTransaction.setWalletAddress(accountChargeNumberCmd.getWalletAddress());
        accountLockChargeTransaction.setAccountId(account.getId());
        accountLockChargeTransaction.setBeforeNumber(beforeChargeLockNumber);
        accountLockChargeTransaction.setTransactionTime(System.currentTimeMillis());
        accountLockChargeTransaction.setNumber(accountChargeNumberCmd.getNumber());
        accountLockChargeTransaction.setAfterNumber(account.getChargeLockNumber());
        accountLockChargeTransaction.setAccountType(account.getType());
        accountLockChargeTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountLockChargeTransaction.setTransactionType(AccountTransactionType.RELEASE_LOCK_CHARGE.getCode());
        accountLockChargeTransaction.setHash(accountChargeNumberCmd.getHash());
        accountTransactionMapper.insert(accountChargeTransaction);

        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> lockChargeNumber(AccountLockChargeNumberCmd accountLockChargeNumberCmd) {
        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Account::getWalletAddress, accountLockChargeNumberCmd.getWalletAddress());
        queryWrapper.eq(Account::getType, accountLockChargeNumberCmd.getType());
        queryWrapper.last("FOR UPDATE");

        Account account = accountMapper.selectOne(queryWrapper);
        if (account == null) {
            return SingleResponse.buildFailure("账户不存在");
        }

        String beforeChargeLockNumber = account.getChargeLockNumber();

        account.setChargeLockNumber(String.valueOf(Long.parseLong(account.getChargeLockNumber()) + Long.parseLong(accountLockChargeNumberCmd.getNumber())));

        account.setUpdateTime(System.currentTimeMillis());
        int updateCount = accountMapper.updateById(account);

        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("乐观锁异常");
        }

        AccountTransaction accountChargeTransaction = new AccountTransaction();
        accountChargeTransaction.setWalletAddress(accountLockChargeNumberCmd.getWalletAddress());
        accountChargeTransaction.setAccountId(account.getId());
        accountChargeTransaction.setBeforeNumber(beforeChargeLockNumber);
        accountChargeTransaction.setTransactionTime(System.currentTimeMillis());
        accountChargeTransaction.setNumber(accountLockChargeNumberCmd.getNumber());
        accountChargeTransaction.setAfterNumber(account.getChargeLockNumber());
        accountChargeTransaction.setAccountType(account.getType());
        accountChargeTransaction.setStatus(AccountTransactionStatusEnum.DEALING.getCode());
        accountChargeTransaction.setTransactionType(AccountTransactionType.LOCK_CHARGE.getCode());
        accountChargeTransaction.setHash(accountLockChargeNumberCmd.getHash());
        accountTransactionMapper.insert(accountChargeTransaction);


        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> withdrawNumber(AccountWithdrawNumberCmd accountWithdrawNumberCmd) {
        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Account::getWalletAddress, accountWithdrawNumberCmd.getWalletAddress());
        queryWrapper.eq(Account::getType, accountWithdrawNumberCmd.getType());
        queryWrapper.last("FOR UPDATE");

        Account account = accountMapper.selectOne(queryWrapper);
        if (account == null) {
            return SingleResponse.buildFailure("账户不存在");
        }

        String beforeWithdrawNumber = account.getWithdrawNumber();
        String beforeWithdrawLockNumber = account.getWithdrawLockNumber();

        account.setWithdrawNumber(String.valueOf(Long.parseLong(account.getWithdrawNumber()) + Long.parseLong(accountWithdrawNumberCmd.getNumber())));
        account.setWithdrawLockNumber(String.valueOf(Long.parseLong(account.getWithdrawLockNumber()) - Long.parseLong(accountWithdrawNumberCmd.getNumber())));
        account.setUpdateTime(System.currentTimeMillis());
        int updateCount = accountMapper.updateById(account);

        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("乐观锁异常");
        }

        AccountTransaction accountWithdrawTransaction = new AccountTransaction();
        accountWithdrawTransaction.setWalletAddress(accountWithdrawNumberCmd.getWalletAddress());
        accountWithdrawTransaction.setAccountId(account.getId());
        accountWithdrawTransaction.setBeforeNumber(beforeWithdrawNumber);
        accountWithdrawTransaction.setTransactionTime(System.currentTimeMillis());
        accountWithdrawTransaction.setNumber(accountWithdrawNumberCmd.getNumber());
        accountWithdrawTransaction.setAfterNumber(account.getWithdrawNumber());
        accountWithdrawTransaction.setAccountType(account.getType());
        accountWithdrawTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountWithdrawTransaction.setTransactionType(AccountTransactionType.WITHDRAW.getCode());
        accountWithdrawTransaction.setHash(accountWithdrawNumberCmd.getHash());
        accountTransactionMapper.insert(accountWithdrawTransaction);

        AccountTransaction accountLockWithdrawTransaction = new AccountTransaction();
        accountLockWithdrawTransaction.setWalletAddress(accountWithdrawNumberCmd.getWalletAddress());
        accountLockWithdrawTransaction.setAccountId(account.getId());
        accountLockWithdrawTransaction.setBeforeNumber(beforeWithdrawLockNumber);
        accountLockWithdrawTransaction.setTransactionTime(System.currentTimeMillis());
        accountLockWithdrawTransaction.setNumber(accountWithdrawNumberCmd.getNumber());
        accountLockWithdrawTransaction.setAfterNumber(account.getWithdrawLockNumber());
        accountLockWithdrawTransaction.setAccountType(account.getType());
        accountLockWithdrawTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountLockWithdrawTransaction.setTransactionType(AccountTransactionType.RELEASE_LOCK_WITHDRAW.getCode());
        accountLockWithdrawTransaction.setHash(accountWithdrawNumberCmd.getHash());
        accountTransactionMapper.insert(accountLockWithdrawTransaction);


        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> lockWithdrawNumber(AccountLockWithdrawNumberCmd accountLockWithdrawNumberCmd) {
        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Account::getWalletAddress, accountLockWithdrawNumberCmd.getWalletAddress());
        queryWrapper.eq(Account::getType, accountLockWithdrawNumberCmd.getType());
        queryWrapper.last("FOR UPDATE");

        Account account = accountMapper.selectOne(queryWrapper);
        if (account == null) {
            return SingleResponse.buildFailure("账户不存在");
        }


        String beforeWithdrawLockNumber = account.getWithdrawLockNumber();

        account.setWithdrawLockNumber(String.valueOf(Long.parseLong(account.getWithdrawLockNumber()) + Long.parseLong(accountLockWithdrawNumberCmd.getNumber())));
        account.setUpdateTime(System.currentTimeMillis());
        int updateCount = accountMapper.updateById(account);

        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("乐观锁异常");
        }


        AccountTransaction accountLockWithdrawTransaction = new AccountTransaction();
        accountLockWithdrawTransaction.setWalletAddress(accountLockWithdrawNumberCmd.getWalletAddress());
        accountLockWithdrawTransaction.setAccountId(account.getId());
        accountLockWithdrawTransaction.setBeforeNumber(beforeWithdrawLockNumber);
        accountLockWithdrawTransaction.setTransactionTime(System.currentTimeMillis());
        accountLockWithdrawTransaction.setNumber(accountLockWithdrawNumberCmd.getNumber());
        accountLockWithdrawTransaction.setAfterNumber(account.getWithdrawLockNumber());
        accountLockWithdrawTransaction.setAccountType(account.getType());
        accountLockWithdrawTransaction.setStatus(AccountTransactionStatusEnum.DEALING.getCode());
        accountLockWithdrawTransaction.setTransactionType(AccountTransactionType.LOCK_WITHDRAW.getCode());
        accountLockWithdrawTransaction.setHash(accountLockWithdrawNumberCmd.getHash());
        accountTransactionMapper.insert(accountLockWithdrawTransaction);


        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> purchaseMinerProjectNumber(AccountDeductCmd accountDeductCmd) {

        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Account::getWalletAddress, accountDeductCmd.getWalletAddress());
        queryWrapper.eq(Account::getType, accountDeductCmd.getAccountType());

        Account account = accountMapper.selectOne(queryWrapper);
        if (account == null) {
            return SingleResponse.buildFailure(account.getType() + "账户不存在");
        }

        BigDecimal balance = new BigDecimal(account.getChargeNumber())
                .add(new BigDecimal(account.getStaticReward()))
                .add(new BigDecimal(account.getDynamicReward()));

        BigDecimal deductNumber = new BigDecimal(accountDeductCmd.getNumber());

        if (balance.compareTo(deductNumber) < 0) {
            return SingleResponse.buildFailure(account.getType() + "账户积分不足");
        }

        // 使用充值积分扣除
        if (deductNumber.compareTo(new BigDecimal(0)) > 0) {

            String beforeChargeNumber = account.getChargeNumber();
            if (new BigDecimal(account.getChargeNumber()).compareTo(deductNumber) >= 0) {
                account.setChargeNumber(String.valueOf(new BigDecimal(account.getChargeNumber()).subtract(deductNumber).longValue()));
                deductNumber = BigDecimal.ZERO;
            } else {
                deductNumber = deductNumber.subtract(new BigDecimal(account.getChargeNumber()));
                account.setChargeNumber("0");
            }

            AccountTransaction accountChargeTransaction = new AccountTransaction();
            accountChargeTransaction.setWalletAddress(accountDeductCmd.getWalletAddress());
            accountChargeTransaction.setAccountId(account.getId());
            accountChargeTransaction.setBeforeNumber(beforeChargeNumber);
            accountChargeTransaction.setTransactionTime(System.currentTimeMillis());
            accountChargeTransaction.setNumber(accountDeductCmd.getNumber());
            accountChargeTransaction.setAfterNumber(account.getChargeNumber());
            accountChargeTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
            accountChargeTransaction.setTransactionType(AccountTransactionType.DEDUCT_CHARGE.getCode());
            accountChargeTransaction.setOrderId(accountDeductCmd.getOrderId());
            accountChargeTransaction.setAccountType(account.getType());
            accountTransactionMapper.insert(accountChargeTransaction);
        }

        // 使用静态积分扣除
        if (deductNumber.compareTo(new BigDecimal(0)) > 0) {

            String beforeStaticReward = account.getStaticReward();
            if (new BigDecimal(account.getStaticReward()).compareTo(deductNumber) >= 0) {
                account.setStaticReward(String.valueOf(new BigDecimal(account.getStaticReward()).subtract(deductNumber).longValue()));
                deductNumber = BigDecimal.ZERO;
            } else {
                deductNumber = deductNumber.subtract(new BigDecimal(account.getStaticReward()));
                account.setStaticReward("0");
            }

            AccountTransaction accountStaticTransaction = new AccountTransaction();
            accountStaticTransaction.setWalletAddress(accountDeductCmd.getWalletAddress());
            accountStaticTransaction.setAccountId(account.getId());
            accountStaticTransaction.setBeforeNumber(beforeStaticReward);
            accountStaticTransaction.setTransactionTime(System.currentTimeMillis());
            accountStaticTransaction.setNumber(accountDeductCmd.getNumber());
            accountStaticTransaction.setAfterNumber(account.getStaticReward());
            accountStaticTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
            accountStaticTransaction.setTransactionType(AccountTransactionType.DEDUCT_STATIC_REWARD.getCode());
            accountStaticTransaction.setOrderId(accountDeductCmd.getOrderId());
            accountStaticTransaction.setAccountType(account.getType());
            accountTransactionMapper.insert(accountStaticTransaction);
        }

        // 使用动态积分扣除
        if (deductNumber.compareTo(new BigDecimal(0)) > 0) {

            String beforeDynamicReward = account.getDynamicReward();
            if (new BigDecimal(account.getDynamicReward()).compareTo(deductNumber) >= 0) {
                account.setDynamicReward(String.valueOf(new BigDecimal(account.getDynamicReward()).subtract(deductNumber).longValue()));
                deductNumber = BigDecimal.ZERO;
            } else {
                deductNumber = deductNumber.subtract(new BigDecimal(account.getDynamicReward()));
                account.setDynamicReward("0");
            }

            AccountTransaction accountDynamicTransaction = new AccountTransaction();
            accountDynamicTransaction.setWalletAddress(accountDeductCmd.getWalletAddress());
            accountDynamicTransaction.setAccountId(account.getId());
            accountDynamicTransaction.setBeforeNumber(beforeDynamicReward);
            accountDynamicTransaction.setTransactionTime(System.currentTimeMillis());
            accountDynamicTransaction.setNumber(accountDeductCmd.getNumber());
            accountDynamicTransaction.setAfterNumber(account.getDynamicReward());
            accountDynamicTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
            accountDynamicTransaction.setTransactionType(AccountTransactionType.DEDUCT_DYNAMIC_REWARD.getCode());
            accountDynamicTransaction.setOrderId(accountDeductCmd.getOrderId());
            accountDynamicTransaction.setAccountType(account.getType());
            accountTransactionMapper.insert(accountDynamicTransaction);
        }

        if (deductNumber.compareTo(new BigDecimal(0)) > 0) {
            throw new OptimisticLockingFailureException("扣除积分失败");
        }

        account.setUpdateTime(System.currentTimeMillis());
        int updateCount = accountMapper.updateById(account);
        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("扣除积分失败");
        }

        return SingleResponse.buildSuccess();

    }
}
