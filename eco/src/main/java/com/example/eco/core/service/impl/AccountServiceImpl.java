package com.example.eco.core.service.impl;

import com.alibaba.excel.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.bean.dto.AccountDTO;
import com.example.eco.common.AccountTransactionStatusEnum;
import com.example.eco.common.AccountTransactionType;
import com.example.eco.common.AccountType;
import com.example.eco.common.SystemConfigEnum;
import com.example.eco.core.service.AccountService;
import com.example.eco.model.entity.Account;
import com.example.eco.model.entity.AccountTransaction;
import com.example.eco.model.entity.PurchaseMinerProjectReward;
import com.example.eco.model.entity.SystemConfig;
import com.example.eco.model.mapper.AccountMapper;
import com.example.eco.model.mapper.AccountTransactionMapper;
import com.example.eco.model.mapper.SystemConfigMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class AccountServiceImpl implements AccountService {

    @Resource
    private AccountMapper accountMapper;
    @Resource
    private AccountTransactionMapper accountTransactionMapper;
    @Resource
    private SystemConfigMapper systemConfigMapper;

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
            existingEcoAccount.setNumber("0");
            existingEcoAccount.setServiceNumber("0");
            existingEcoAccount.setServiceLockNumber("0");
            existingEcoAccount.setStaticRewardPrice("0");
            existingEcoAccount.setDynamicRewardPrice("0");
            existingEcoAccount.setType(AccountType.ECO.getCode());
            existingEcoAccount.setCreateTime(System.currentTimeMillis());
            existingEcoAccount.setUpdateTime(System.currentTimeMillis());
            accountMapper.insert(existingEcoAccount);
        }


        LambdaQueryWrapper<Account> esgQueryWrapper = new LambdaQueryWrapper<>();
        esgQueryWrapper.eq(Account::getWalletAddress, accountCreateCmd.getWalletAddress());
        esgQueryWrapper.eq(Account::getType, AccountType.ESG.getCode());
        Account existingEsgAccount = accountMapper.selectOne(esgQueryWrapper);
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
            existingEsgAccount.setNumber("0");
            existingEsgAccount.setServiceNumber("0");
            existingEsgAccount.setServiceLockNumber("0");
            existingEsgAccount.setStaticRewardPrice("0");
            existingEsgAccount.setDynamicRewardPrice("0");
            existingEsgAccount.setType(AccountType.ESG.getCode());
            existingEsgAccount.setCreateTime(System.currentTimeMillis());
            existingEsgAccount.setUpdateTime(System.currentTimeMillis());
            accountMapper.insert(existingEsgAccount);
        }


        return SingleResponse.buildSuccess();
    }

    @Override
    public MultiResponse<AccountDTO> list(AccountPageQry accountPageQry) {

        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StringUtils.isNotBlank(accountPageQry.getWalletAddress()), Account::getWalletAddress, accountPageQry.getWalletAddress());
        queryWrapper.eq(StringUtils.isNotBlank(accountPageQry.getType()), Account::getType, accountPageQry.getType());

        Page<Account> accountPage = accountMapper.selectPage(Page.of(accountPageQry.getPageNum(), accountPageQry.getPageSize()), queryWrapper);

        if (CollectionUtils.isEmpty(accountPage.getRecords())) {
            return MultiResponse.buildSuccess();
        }

        List<AccountDTO> accountDTOList = new ArrayList<>();

        for (Account account : accountPage.getRecords()) {
            AccountDTO accountDTO = new AccountDTO();
            BeanUtils.copyProperties(account, accountDTO);
            accountDTO.setTypeName(AccountType.of(account.getType()).getName());

            accountDTOList.add(accountDTO);
        }
        return MultiResponse.of(accountDTOList, (int) accountPage.getTotal());
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> addStaticNumber(AccountStaticNumberCmd accountStaticNumberCmd) {

//        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(Account::getWalletAddress, accountStaticNumberCmd.getWalletAddress());
//        queryWrapper.eq(Account::getType, accountStaticNumberCmd.getType());
//        queryWrapper.last("FOR UPDATE");
//
//        Account account = accountMapper.selectOne(queryWrapper);
//        if (account == null) {
//            return SingleResponse.buildFailure("账户不存在");
//        }

        Account account = getOrCreate(accountStaticNumberCmd.getWalletAddress(), accountStaticNumberCmd.getType());

        LambdaQueryWrapper<SystemConfig> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(SystemConfig::getName,SystemConfigEnum.ECO_PRICE.getCode());

        SystemConfig systemConfig = systemConfigMapper.selectOne(lambdaQueryWrapper);
        if (Objects.isNull(systemConfig)){
            return SingleResponse.buildFailure("ECO价格未设置");
        }

        String beforeNumber = account.getNumber();

        String beforeStaticReward = account.getStaticReward();

        BigDecimal price = new BigDecimal(accountStaticNumberCmd.getNumber()).multiply(new BigDecimal(systemConfig.getValue()));

        account.setStaticRewardPrice(String.valueOf(new BigDecimal(account.getStaticRewardPrice()).add(price)));

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
        accountTransaction.setOrder(accountStaticNumberCmd.getOrder());
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
        accountStaticTransaction.setOrder(accountStaticNumberCmd.getOrder());
        accountTransactionMapper.insert(accountStaticTransaction);

        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> addDynamicNumber(AccountDynamicNumberCmd accountDynamicNumberCmd) {
//        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(Account::getWalletAddress, accountDynamicNumberCmd.getWalletAddress());
//        queryWrapper.eq(Account::getType, accountDynamicNumberCmd.getType());
//        queryWrapper.last("FOR UPDATE");
//
//        Account account = accountMapper.selectOne(queryWrapper);
//        if (account == null) {
//            return SingleResponse.buildFailure("账户不存在");
//        }

        Account account = getOrCreate(accountDynamicNumberCmd.getWalletAddress(), accountDynamicNumberCmd.getType());

        LambdaQueryWrapper<SystemConfig> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(SystemConfig::getName,SystemConfigEnum.ECO_PRICE.getCode());

        SystemConfig systemConfig = systemConfigMapper.selectOne(lambdaQueryWrapper);
        if (Objects.isNull(systemConfig)){
            return SingleResponse.buildFailure("ECO价格未设置");
        }

        String beforeNumber = account.getNumber();

        String beforeDynamicReward = account.getDynamicReward();

        BigDecimal price = new BigDecimal(accountDynamicNumberCmd.getNumber()).multiply(new BigDecimal(systemConfig.getValue()));

        account.setDynamicRewardPrice(String.valueOf(new BigDecimal(account.getDynamicRewardPrice()).add(price)));

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
        accountTransaction.setOrder(accountDynamicNumberCmd.getOrder());
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
        accountDynamicTransaction.setOrder(accountDynamicNumberCmd.getOrder());
        accountTransactionMapper.insert(accountDynamicTransaction);

        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> buyNumber(AccountBuyNumberCmd accountBuyNumberCmd) {
//        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(Account::getWalletAddress, accountBuyNumberCmd.getWalletAddress());
//        queryWrapper.eq(Account::getType, accountBuyNumberCmd.getType());
//        queryWrapper.last("FOR UPDATE");
//
//        Account account = accountMapper.selectOne(queryWrapper);
//        if (account == null) {
//            return SingleResponse.buildFailure("账户不存在");
//        }

        Account account = getOrCreate(accountBuyNumberCmd.getWalletAddress(), accountBuyNumberCmd.getType());

        String beforeBuyLockNumber = account.getBuyLockNumber();
        account.setBuyLockNumber(String.valueOf(new BigDecimal(account.getBuyLockNumber()).add(new BigDecimal(accountBuyNumberCmd.getNumber()))));
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
        accountLockBuyTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountLockBuyTransaction.setTransactionType(AccountTransactionType.LOCK_BUY.getCode());
        accountLockBuyTransaction.setOrder(accountBuyNumberCmd.getOrder());

        accountTransactionMapper.insert(accountLockBuyTransaction);

        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> releaseLockBuyNumber(AccountReleaseLockBuyNumberCmd accountReleaseLockBuyNumberCmd) {

        LambdaQueryWrapper<AccountTransaction> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(AccountTransaction::getWalletAddress, accountReleaseLockBuyNumberCmd.getWalletAddress());
        lambdaQueryWrapper.eq(AccountTransaction::getOrder, accountReleaseLockBuyNumberCmd.getOrder());
        lambdaQueryWrapper.eq(AccountTransaction::getTransactionType, AccountTransactionType.LOCK_BUY.getCode());
        lambdaQueryWrapper.orderByDesc(AccountTransaction::getId);
        lambdaQueryWrapper.last("LIMIT 1");

        AccountTransaction lockBuyTransaction = accountTransactionMapper.selectOne(lambdaQueryWrapper);

        Account account = accountMapper.selectById(lockBuyTransaction.getAccountId());
        if (account == null) {
            return SingleResponse.buildFailure("账户不存在");
        }

        String beforeNumber = account.getNumber();
        String beforeBuyNumber = account.getBuyNumber();
        String beforeLockBuyNumber = account.getBuyLockNumber();

        account.setNumber(String.valueOf(new BigDecimal(account.getNumber()).add(new BigDecimal(lockBuyTransaction.getNumber()))));
        account.setBuyNumber(String.valueOf(new BigDecimal(account.getBuyNumber()).add(new BigDecimal(lockBuyTransaction.getNumber()))));
        account.setBuyLockNumber(String.valueOf(new BigDecimal(account.getBuyLockNumber()).subtract(new BigDecimal(lockBuyTransaction.getNumber()))));
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
        accountTransaction.setNumber(lockBuyTransaction.getNumber());
        accountTransaction.setAfterNumber(account.getNumber());
        accountTransaction.setAccountType(account.getType());
        accountTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountTransaction.setTransactionType(AccountTransactionType.ADD_NUMBER.getCode());
        accountTransaction.setOrder(accountReleaseLockBuyNumberCmd.getOrder());

        accountTransactionMapper.insert(accountTransaction);

        AccountTransaction accountBuyTransaction = new AccountTransaction();
        accountBuyTransaction.setWalletAddress(accountReleaseLockBuyNumberCmd.getWalletAddress());
        accountBuyTransaction.setAccountId(account.getId());
        accountBuyTransaction.setBeforeNumber(beforeBuyNumber);
        accountBuyTransaction.setTransactionTime(System.currentTimeMillis());
        accountBuyTransaction.setNumber(lockBuyTransaction.getNumber());
        accountBuyTransaction.setAfterNumber(account.getBuyNumber());
        accountBuyTransaction.setAccountType(account.getType());
        accountBuyTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountBuyTransaction.setTransactionType(AccountTransactionType.BUY.getCode());
        accountBuyTransaction.setOrder(accountReleaseLockBuyNumberCmd.getOrder());

        accountTransactionMapper.insert(accountBuyTransaction);

        AccountTransaction accountReleaseBuyTransaction = new AccountTransaction();
        accountReleaseBuyTransaction.setWalletAddress(accountReleaseLockBuyNumberCmd.getWalletAddress());
        accountReleaseBuyTransaction.setAccountId(account.getId());
        accountReleaseBuyTransaction.setBeforeNumber(beforeLockBuyNumber);
        accountReleaseBuyTransaction.setTransactionTime(System.currentTimeMillis());
        accountReleaseBuyTransaction.setNumber(lockBuyTransaction.getNumber());
        accountReleaseBuyTransaction.setAfterNumber(account.getBuyLockNumber());
        accountReleaseBuyTransaction.setAccountType(account.getType());
        accountReleaseBuyTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountReleaseBuyTransaction.setTransactionType(AccountTransactionType.RELEASE_LOCK_BUY.getCode());
        accountReleaseBuyTransaction.setOrder(accountReleaseLockBuyNumberCmd.getOrder());

        accountTransactionMapper.insert(accountReleaseBuyTransaction);


        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> rollbackLockBuyNumber(RollbackLockBuyNumberCmd rollbackLockBuyNumberCmd) {

        LambdaQueryWrapper<AccountTransaction> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(AccountTransaction::getWalletAddress, rollbackLockBuyNumberCmd.getWalletAddress());
        lambdaQueryWrapper.eq(AccountTransaction::getOrder, rollbackLockBuyNumberCmd.getOrder());
        lambdaQueryWrapper.eq(AccountTransaction::getTransactionType, AccountTransactionType.LOCK_BUY.getCode());
        lambdaQueryWrapper.orderByDesc(AccountTransaction::getId);
        lambdaQueryWrapper.last("LIMIT 1");

        AccountTransaction lockBuyTransaction = accountTransactionMapper.selectOne(lambdaQueryWrapper);

        Account account = accountMapper.selectById(lockBuyTransaction.getAccountId());


        String beforeBuyLockNumber = account.getBuyLockNumber();

        account.setBuyLockNumber(String.valueOf(new BigDecimal(account.getBuyLockNumber()).subtract(new BigDecimal(lockBuyTransaction.getNumber()))));
        account.setUpdateTime(System.currentTimeMillis());
        accountMapper.updateById(account);

        AccountTransaction accountRollbackBuyLockTransaction = new AccountTransaction();
        accountRollbackBuyLockTransaction.setWalletAddress(rollbackLockBuyNumberCmd.getWalletAddress());
        accountRollbackBuyLockTransaction.setAccountId(account.getId());
        accountRollbackBuyLockTransaction.setBeforeNumber(beforeBuyLockNumber);
        accountRollbackBuyLockTransaction.setTransactionTime(System.currentTimeMillis());
        accountRollbackBuyLockTransaction.setNumber(lockBuyTransaction.getNumber());
        accountRollbackBuyLockTransaction.setAfterNumber(account.getBuyLockNumber());
        accountRollbackBuyLockTransaction.setAccountType(account.getType());
        accountRollbackBuyLockTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountRollbackBuyLockTransaction.setTransactionType(AccountTransactionType.ROLLBACK_LOCK_BUY.getCode());
        accountRollbackBuyLockTransaction.setOrder(rollbackLockBuyNumberCmd.getOrder());

        accountTransactionMapper.insert(accountRollbackBuyLockTransaction);

        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> sellNumber(AccountSellNumberCmd accountSellNumberCmd) {
//        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(Account::getWalletAddress, accountSellNumberCmd.getWalletAddress());
//        queryWrapper.eq(Account::getType, accountSellNumberCmd.getType());
//        queryWrapper.last("FOR UPDATE");
//
//        Account account = accountMapper.selectOne(queryWrapper);
//        if (account == null) {
//            return SingleResponse.buildFailure("账户不存在");
//        }

        Account account = getOrCreate(accountSellNumberCmd.getWalletAddress(), accountSellNumberCmd.getType());

        BigDecimal balance = new BigDecimal(account.getNumber());

        if (balance.compareTo(new BigDecimal(accountSellNumberCmd.getNumber())) < 0) {
            return SingleResponse.buildFailure("账户余额不足");
        }


        BigDecimal canSellNumber = new BigDecimal(account.getStaticReward())
                .add(new BigDecimal(account.getDynamicReward()))
                .subtract(new BigDecimal(account.getSellLockNumber()))
                .subtract(new BigDecimal(account.getSellNumber()));

        if (canSellNumber.compareTo(new BigDecimal(accountSellNumberCmd.getNumber())) < 0) {
            return SingleResponse.buildFailure("账户额度不足");
        }

        String beforeNumber = account.getNumber();
        String beforeSellLockNumber = account.getSellLockNumber();

        account.setNumber(String.valueOf(new BigDecimal(account.getNumber()).subtract(new BigDecimal(accountSellNumberCmd.getNumber()))));
        account.setSellLockNumber(String.valueOf(new BigDecimal(account.getSellLockNumber()).add(new BigDecimal(accountSellNumberCmd.getNumber()))));
        account.setUpdateTime(System.currentTimeMillis());
        int updateCount = accountMapper.updateById(account);

        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("乐观锁异常");
        }

        AccountTransaction accountTransaction = new AccountTransaction();
        accountTransaction.setWalletAddress(accountSellNumberCmd.getWalletAddress());
        accountTransaction.setAccountId(account.getId());
        accountTransaction.setBeforeNumber(beforeNumber);
        accountTransaction.setTransactionTime(System.currentTimeMillis());
        accountTransaction.setNumber(accountSellNumberCmd.getNumber());
        accountTransaction.setAfterNumber(account.getNumber());
        accountTransaction.setAccountType(account.getType());
        accountTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountTransaction.setTransactionType(AccountTransactionType.DEDUCT_NUMBER.getCode());
        accountTransaction.setOrder(accountSellNumberCmd.getOrder());
        accountTransactionMapper.insert(accountTransaction);

        AccountTransaction accountLockSellTransaction = new AccountTransaction();
        accountLockSellTransaction.setWalletAddress(accountSellNumberCmd.getWalletAddress());
        accountLockSellTransaction.setAccountId(account.getId());
        accountLockSellTransaction.setBeforeNumber(beforeSellLockNumber);
        accountLockSellTransaction.setTransactionTime(System.currentTimeMillis());
        accountLockSellTransaction.setNumber(accountSellNumberCmd.getNumber());
        accountLockSellTransaction.setAfterNumber(account.getSellLockNumber());
        accountLockSellTransaction.setAccountType(account.getType());
        accountLockSellTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountLockSellTransaction.setTransactionType(AccountTransactionType.LOCK_SELL.getCode());
        accountLockSellTransaction.setOrder(accountSellNumberCmd.getOrder());
        accountTransactionMapper.insert(accountLockSellTransaction);

        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> releaseLockSellNumber(AccountReleaseLockSellNumberCmd accountReleaseLockSellNumberCmd) {

        LambdaQueryWrapper<AccountTransaction> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(AccountTransaction::getWalletAddress, accountReleaseLockSellNumberCmd.getWalletAddress());
        lambdaQueryWrapper.eq(AccountTransaction::getOrder, accountReleaseLockSellNumberCmd.getOrder());
        lambdaQueryWrapper.eq(AccountTransaction::getTransactionType, AccountTransactionType.LOCK_SELL.getCode());
        lambdaQueryWrapper.orderByDesc(AccountTransaction::getId);
        lambdaQueryWrapper.last("LIMIT 1");

        AccountTransaction lockSellTransaction = accountTransactionMapper.selectOne(lambdaQueryWrapper);


        Account account = accountMapper.selectById(lockSellTransaction.getAccountId());
        if (account == null) {
            return SingleResponse.buildFailure("账户不存在");
        }

        String beforeSellNumber = account.getSellNumber();
        String beforeLockSellNumber = account.getSellLockNumber();

        account.setSellNumber(String.valueOf(new BigDecimal(account.getSellNumber()).add(new BigDecimal(lockSellTransaction.getNumber()))));
        account.setSellLockNumber(String.valueOf(new BigDecimal(account.getSellLockNumber()).subtract(new BigDecimal(lockSellTransaction.getNumber()))));
        account.setUpdateTime(System.currentTimeMillis());

        int updateCount = accountMapper.updateById(account);

        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("乐观锁异常");
        }


        AccountTransaction accountSellTransaction = new AccountTransaction();
        accountSellTransaction.setWalletAddress(lockSellTransaction.getWalletAddress());
        accountSellTransaction.setAccountId(account.getId());
        accountSellTransaction.setBeforeNumber(beforeSellNumber);
        accountSellTransaction.setTransactionTime(System.currentTimeMillis());
        accountSellTransaction.setNumber(lockSellTransaction.getNumber());
        accountSellTransaction.setAfterNumber(account.getSellNumber());
        accountSellTransaction.setAccountType(account.getType());
        accountSellTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountSellTransaction.setTransactionType(AccountTransactionType.SELL.getCode());
        accountSellTransaction.setOrder(lockSellTransaction.getOrder());
        accountTransactionMapper.insert(accountSellTransaction);

        AccountTransaction accountReleaseSellTransaction = new AccountTransaction();
        accountReleaseSellTransaction.setWalletAddress(lockSellTransaction.getWalletAddress());
        accountReleaseSellTransaction.setAccountId(account.getId());
        accountReleaseSellTransaction.setBeforeNumber(beforeLockSellNumber);
        accountReleaseSellTransaction.setTransactionTime(System.currentTimeMillis());
        accountReleaseSellTransaction.setNumber(lockSellTransaction.getNumber());
        accountReleaseSellTransaction.setAfterNumber(account.getSellLockNumber());
        accountReleaseSellTransaction.setAccountType(account.getType());
        accountReleaseSellTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountReleaseSellTransaction.setTransactionType(AccountTransactionType.RELEASE_LOCK_SELL.getCode());
        accountReleaseSellTransaction.setOrder(lockSellTransaction.getOrder());
        accountTransactionMapper.insert(accountReleaseSellTransaction);


        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> rollbackLockSellNumber(RollbackLockSellNumberCmd rollbackLockSellNumberCmd) {


        LambdaQueryWrapper<AccountTransaction> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(AccountTransaction::getWalletAddress, rollbackLockSellNumberCmd.getWalletAddress());
        lambdaQueryWrapper.eq(AccountTransaction::getOrder, rollbackLockSellNumberCmd.getOrder());
        lambdaQueryWrapper.eq(AccountTransaction::getTransactionType, AccountTransactionType.LOCK_SELL.getCode());
        lambdaQueryWrapper.orderByDesc(AccountTransaction::getId);
        lambdaQueryWrapper.last("LIMIT 1");

        AccountTransaction lockSellTransaction = accountTransactionMapper.selectOne(lambdaQueryWrapper);

        Account account = accountMapper.selectById(lockSellTransaction.getAccountId());


        String beforeSellLockNumber = account.getSellLockNumber();
        String beforeNumber = account.getNumber();

        account.setSellLockNumber(String.valueOf(new BigDecimal(account.getSellLockNumber()).subtract(new BigDecimal(lockSellTransaction.getNumber()))));
        account.setNumber(String.valueOf(new BigDecimal(account.getNumber()).add(new BigDecimal(lockSellTransaction.getNumber()))));
        account.setUpdateTime(System.currentTimeMillis());
        accountMapper.updateById(account);

        AccountTransaction accountTransaction = new AccountTransaction();
        accountTransaction.setWalletAddress(rollbackLockSellNumberCmd.getWalletAddress());
        accountTransaction.setAccountId(account.getId());
        accountTransaction.setBeforeNumber(beforeNumber);
        accountTransaction.setTransactionTime(System.currentTimeMillis());
        accountTransaction.setNumber(lockSellTransaction.getNumber());
        accountTransaction.setAfterNumber(account.getNumber());
        accountTransaction.setAccountType(account.getType());
        accountTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountTransaction.setTransactionType(AccountTransactionType.ADD_NUMBER.getCode());
        accountTransaction.setOrder(rollbackLockSellNumberCmd.getOrder());

        accountTransactionMapper.insert(accountTransaction);

        AccountTransaction accountRollbackSellLockTransaction = new AccountTransaction();
        accountRollbackSellLockTransaction.setWalletAddress(rollbackLockSellNumberCmd.getWalletAddress());
        accountRollbackSellLockTransaction.setAccountId(account.getId());
        accountRollbackSellLockTransaction.setBeforeNumber(beforeSellLockNumber);
        accountRollbackSellLockTransaction.setTransactionTime(System.currentTimeMillis());
        accountRollbackSellLockTransaction.setNumber(lockSellTransaction.getNumber());
        accountRollbackSellLockTransaction.setAfterNumber(account.getSellLockNumber());
        accountRollbackSellLockTransaction.setAccountType(account.getType());
        accountRollbackSellLockTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountRollbackSellLockTransaction.setTransactionType(AccountTransactionType.ROLLBACK_LOCK_SELL.getCode());
        accountRollbackSellLockTransaction.setOrder(rollbackLockSellNumberCmd.getOrder());

        accountTransactionMapper.insert(accountRollbackSellLockTransaction);


        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> chargeNumber(AccountChargeNumberCmd accountChargeNumberCmd) {

        Account account = getOrCreate(accountChargeNumberCmd.getWalletAddress(), accountChargeNumberCmd.getType());

        String beforeChargeLockNumber = account.getChargeLockNumber();

        account.setChargeLockNumber(String.valueOf(new BigDecimal(account.getChargeLockNumber()).add(new BigDecimal(accountChargeNumberCmd.getNumber()))));

        account.setUpdateTime(System.currentTimeMillis());
        int updateCount = accountMapper.updateById(account);

        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("乐观锁异常");
        }

        AccountTransaction accountLockChargeTransaction = new AccountTransaction();
        accountLockChargeTransaction.setWalletAddress(accountChargeNumberCmd.getWalletAddress());
        accountLockChargeTransaction.setAccountId(account.getId());
        accountLockChargeTransaction.setBeforeNumber(beforeChargeLockNumber);
        accountLockChargeTransaction.setTransactionTime(System.currentTimeMillis());
        accountLockChargeTransaction.setNumber(accountChargeNumberCmd.getNumber());
        accountLockChargeTransaction.setAfterNumber(account.getChargeLockNumber());
        accountLockChargeTransaction.setAccountType(account.getType());
        accountLockChargeTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountLockChargeTransaction.setTransactionType(AccountTransactionType.LOCK_CHARGE.getCode());
        accountLockChargeTransaction.setOrder(accountChargeNumberCmd.getOrder());
        accountLockChargeTransaction.setHash(accountChargeNumberCmd.getHash());

        accountTransactionMapper.insert(accountLockChargeTransaction);

        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> releaseLockChargeNumber(AccountLockChargeNumberCmd accountLockChargeNumberCmd) {

        LambdaQueryWrapper<AccountTransaction> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(AccountTransaction::getWalletAddress, accountLockChargeNumberCmd.getWalletAddress());
        lambdaQueryWrapper.eq(AccountTransaction::getOrder, accountLockChargeNumberCmd.getOrder());
        lambdaQueryWrapper.eq(AccountTransaction::getHash, accountLockChargeNumberCmd.getHash());
        lambdaQueryWrapper.eq(AccountTransaction::getTransactionType, AccountTransactionType.LOCK_CHARGE.getCode());
        lambdaQueryWrapper.orderByDesc(AccountTransaction::getId);
        lambdaQueryWrapper.last("LIMIT 1");

        AccountTransaction lockChargeTransaction = accountTransactionMapper.selectOne(lambdaQueryWrapper);

        Account account = accountMapper.selectById(lockChargeTransaction.getAccountId());
        if (account == null) {
            return SingleResponse.buildFailure("账户不存在");
        }

        String beforeNumber = account.getNumber();
        String beforeChargeNumber = account.getChargeNumber();
        String beforeChargeLockNumber = account.getChargeLockNumber();

        account.setNumber(String.valueOf(new BigDecimal(account.getNumber()).add(new BigDecimal(lockChargeTransaction.getNumber()))));
        account.setChargeNumber(String.valueOf(new BigDecimal(account.getChargeNumber()).add(new BigDecimal(lockChargeTransaction.getNumber()))));
        account.setChargeLockNumber(String.valueOf(new BigDecimal(account.getChargeLockNumber()).subtract(new BigDecimal(lockChargeTransaction.getNumber()))));

        account.setUpdateTime(System.currentTimeMillis());
        int updateCount = accountMapper.updateById(account);

        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("乐观锁异常");
        }

        AccountTransaction accountTransaction = new AccountTransaction();
        accountTransaction.setWalletAddress(accountLockChargeNumberCmd.getWalletAddress());
        accountTransaction.setAccountId(account.getId());
        accountTransaction.setBeforeNumber(beforeNumber);
        accountTransaction.setTransactionTime(System.currentTimeMillis());
        accountTransaction.setNumber(lockChargeTransaction.getNumber());
        accountTransaction.setAfterNumber(account.getNumber());
        accountTransaction.setAccountType(account.getType());
        accountTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountTransaction.setTransactionType(AccountTransactionType.ADD_NUMBER.getCode());
        accountTransaction.setOrder(lockChargeTransaction.getOrder());
        accountTransaction.setHash(accountLockChargeNumberCmd.getHash());
        accountTransactionMapper.insert(accountTransaction);

        AccountTransaction accountChargeTransaction = new AccountTransaction();
        accountChargeTransaction.setWalletAddress(accountLockChargeNumberCmd.getWalletAddress());
        accountChargeTransaction.setAccountId(account.getId());
        accountChargeTransaction.setBeforeNumber(beforeChargeNumber);
        accountChargeTransaction.setTransactionTime(System.currentTimeMillis());
        accountChargeTransaction.setNumber(lockChargeTransaction.getNumber());
        accountChargeTransaction.setAfterNumber(account.getChargeNumber());
        accountChargeTransaction.setAccountType(account.getType());
        accountChargeTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountChargeTransaction.setTransactionType(AccountTransactionType.CHARGE.getCode());
        accountChargeTransaction.setOrder(lockChargeTransaction.getOrder());
        accountChargeTransaction.setHash(accountLockChargeNumberCmd.getHash());
        accountTransactionMapper.insert(accountChargeTransaction);


        AccountTransaction accountReleaseChargeTransaction = new AccountTransaction();
        accountReleaseChargeTransaction.setWalletAddress(accountLockChargeNumberCmd.getWalletAddress());
        accountReleaseChargeTransaction.setAccountId(account.getId());
        accountReleaseChargeTransaction.setBeforeNumber(beforeChargeLockNumber);
        accountReleaseChargeTransaction.setTransactionTime(System.currentTimeMillis());
        accountReleaseChargeTransaction.setNumber(lockChargeTransaction.getNumber());
        accountReleaseChargeTransaction.setAfterNumber(account.getChargeLockNumber());
        accountReleaseChargeTransaction.setAccountType(account.getType());
        accountReleaseChargeTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountReleaseChargeTransaction.setTransactionType(AccountTransactionType.RELEASE_LOCK_CHARGE.getCode());
        accountReleaseChargeTransaction.setHash(accountLockChargeNumberCmd.getHash());
        accountReleaseChargeTransaction.setOrder(lockChargeTransaction.getOrder());
        accountTransactionMapper.insert(accountReleaseChargeTransaction);


        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> rollbackLockChargeNumber(RollbackLockChargeNumberCmd rollbackLockChargeNumberCmd) {

        LambdaQueryWrapper<AccountTransaction> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(AccountTransaction::getWalletAddress, rollbackLockChargeNumberCmd.getWalletAddress());
        lambdaQueryWrapper.eq(AccountTransaction::getOrder, rollbackLockChargeNumberCmd.getOrder());
        lambdaQueryWrapper.eq(AccountTransaction::getHash, rollbackLockChargeNumberCmd.getHash());
        lambdaQueryWrapper.eq(AccountTransaction::getTransactionType, AccountTransactionType.LOCK_CHARGE.getCode());
        lambdaQueryWrapper.orderByDesc(AccountTransaction::getId);
        lambdaQueryWrapper.last("LIMIT 1");

        AccountTransaction lockChargeTransaction = accountTransactionMapper.selectOne(lambdaQueryWrapper);

        Account account = accountMapper.selectById(lockChargeTransaction.getAccountId());
        if (account == null) {
            return SingleResponse.buildFailure("账户不存在");
        }

        String beforeChargeLockNumber = account.getChargeLockNumber();
        account.setChargeLockNumber(String.valueOf(new BigDecimal(account.getChargeLockNumber()).subtract(new BigDecimal(lockChargeTransaction.getNumber()))));
        account.setUpdateTime(System.currentTimeMillis());

        int updateCount = accountMapper.updateById(account);

        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("乐观锁异常");
        }

        AccountTransaction accountRollbackLockChargeTransaction = new AccountTransaction();
        accountRollbackLockChargeTransaction.setWalletAddress(rollbackLockChargeNumberCmd.getWalletAddress());
        accountRollbackLockChargeTransaction.setAccountId(account.getId());
        accountRollbackLockChargeTransaction.setBeforeNumber(beforeChargeLockNumber);
        accountRollbackLockChargeTransaction.setTransactionTime(System.currentTimeMillis());
        accountRollbackLockChargeTransaction.setNumber(lockChargeTransaction.getNumber());
        accountRollbackLockChargeTransaction.setAfterNumber(account.getSellLockNumber());
        accountRollbackLockChargeTransaction.setAccountType(account.getType());
        accountRollbackLockChargeTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountRollbackLockChargeTransaction.setTransactionType(AccountTransactionType.ROLLBACK_LOCK_CHARGE.getCode());
        accountRollbackLockChargeTransaction.setOrder(rollbackLockChargeNumberCmd.getOrder());

        accountTransactionMapper.insert(accountRollbackLockChargeTransaction);

        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> withdrawNumber(AccountWithdrawNumberCmd accountWithdrawNumberCmd) {
//        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(Account::getWalletAddress, accountWithdrawNumberCmd.getWalletAddress());
//        queryWrapper.eq(Account::getType, accountWithdrawNumberCmd.getType());
//        queryWrapper.last("FOR UPDATE");
//
//        Account account = accountMapper.selectOne(queryWrapper);
//        if (account == null) {
//            return SingleResponse.buildFailure("账户不存在");
//        }

        Account account = getOrCreate(accountWithdrawNumberCmd.getWalletAddress(), accountWithdrawNumberCmd.getType());

        String number = account.getNumber();
        if (new BigDecimal(number).compareTo(new BigDecimal(accountWithdrawNumberCmd.getNumber())) < 0) {
            return SingleResponse.buildFailure("账户余额不足");
        }

        String beforeWithdrawLockNumber = account.getWithdrawLockNumber();
        String beforeNumber = account.getNumber();

        account.setNumber(String.valueOf(new BigDecimal(account.getNumber()).subtract(new BigDecimal(accountWithdrawNumberCmd.getNumber()))));
        account.setWithdrawLockNumber(String.valueOf(new BigDecimal(account.getWithdrawLockNumber()).add(new BigDecimal(accountWithdrawNumberCmd.getNumber()))));
        account.setUpdateTime(System.currentTimeMillis());
        int updateCount = accountMapper.updateById(account);

        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("乐观锁异常");
        }

        AccountTransaction accountTransaction = new AccountTransaction();
        accountTransaction.setWalletAddress(accountWithdrawNumberCmd.getWalletAddress());
        accountTransaction.setAccountId(account.getId());
        accountTransaction.setBeforeNumber(beforeNumber);
        accountTransaction.setTransactionTime(System.currentTimeMillis());
        accountTransaction.setNumber(accountWithdrawNumberCmd.getNumber());
        accountTransaction.setAfterNumber(account.getNumber());
        accountTransaction.setAccountType(account.getType());
        accountTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountTransaction.setTransactionType(AccountTransactionType.DEDUCT_NUMBER.getCode());
        accountTransaction.setOrder(accountWithdrawNumberCmd.getOrder());
        accountTransactionMapper.insert(accountTransaction);

        AccountTransaction accountLockWithdrawTransaction = new AccountTransaction();
        accountLockWithdrawTransaction.setWalletAddress(accountWithdrawNumberCmd.getWalletAddress());
        accountLockWithdrawTransaction.setAccountId(account.getId());
        accountLockWithdrawTransaction.setBeforeNumber(beforeWithdrawLockNumber);
        accountLockWithdrawTransaction.setTransactionTime(System.currentTimeMillis());
        accountLockWithdrawTransaction.setNumber(accountWithdrawNumberCmd.getNumber());
        accountLockWithdrawTransaction.setAfterNumber(account.getWithdrawLockNumber());
        accountLockWithdrawTransaction.setAccountType(account.getType());
        accountLockWithdrawTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountLockWithdrawTransaction.setTransactionType(AccountTransactionType.LOCK_WITHDRAW.getCode());
        accountLockWithdrawTransaction.setOrder(accountWithdrawNumberCmd.getOrder());
        accountTransactionMapper.insert(accountLockWithdrawTransaction);


        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> rollbackLockWithdrawNumber(RollbackLockWithdrawNumberCmd rollbackLockWithdrawNumberCmd) {
        LambdaQueryWrapper<AccountTransaction> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(AccountTransaction::getWalletAddress, rollbackLockWithdrawNumberCmd.getWalletAddress());
        lambdaQueryWrapper.eq(AccountTransaction::getOrder, rollbackLockWithdrawNumberCmd.getOrder());
        lambdaQueryWrapper.eq(AccountTransaction::getTransactionType, AccountTransactionType.LOCK_WITHDRAW.getCode());
        lambdaQueryWrapper.orderByDesc(AccountTransaction::getId);
        lambdaQueryWrapper.last("LIMIT 1");

        AccountTransaction lockWithdrawTransaction = accountTransactionMapper.selectOne(lambdaQueryWrapper);

        Account account = accountMapper.selectById(lockWithdrawTransaction.getAccountId());


        String beforeWithdrawLockNumber = account.getWithdrawLockNumber();

        String beforeNumber = account.getNumber();

        account.setWithdrawLockNumber(String.valueOf(new BigDecimal(account.getWithdrawLockNumber()).subtract(new BigDecimal(lockWithdrawTransaction.getNumber()))));
        account.setNumber(String.valueOf(new BigDecimal(account.getNumber()).add(new BigDecimal(lockWithdrawTransaction.getNumber()))));
        account.setUpdateTime(System.currentTimeMillis());
        accountMapper.updateById(account);

        AccountTransaction accountTransaction = new AccountTransaction();
        accountTransaction.setWalletAddress(lockWithdrawTransaction.getWalletAddress());
        accountTransaction.setAccountId(account.getId());
        accountTransaction.setBeforeNumber(beforeNumber);
        accountTransaction.setTransactionTime(System.currentTimeMillis());
        accountTransaction.setNumber(lockWithdrawTransaction.getNumber());
        accountTransaction.setAfterNumber(account.getNumber());
        accountTransaction.setAccountType(account.getType());
        accountTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountTransaction.setTransactionType(AccountTransactionType.ADD_NUMBER.getCode());
        accountTransaction.setOrder(rollbackLockWithdrawNumberCmd.getOrder());

        accountTransactionMapper.insert(accountTransaction);

        AccountTransaction accountRollbackLockWithdrawTransaction = new AccountTransaction();
        accountRollbackLockWithdrawTransaction.setWalletAddress(lockWithdrawTransaction.getWalletAddress());
        accountRollbackLockWithdrawTransaction.setAccountId(account.getId());
        accountRollbackLockWithdrawTransaction.setBeforeNumber(beforeWithdrawLockNumber);
        accountRollbackLockWithdrawTransaction.setTransactionTime(System.currentTimeMillis());
        accountRollbackLockWithdrawTransaction.setNumber(lockWithdrawTransaction.getNumber());
        accountRollbackLockWithdrawTransaction.setAfterNumber(account.getSellLockNumber());
        accountRollbackLockWithdrawTransaction.setAccountType(account.getType());
        accountRollbackLockWithdrawTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountRollbackLockWithdrawTransaction.setTransactionType(AccountTransactionType.ROLLBACK_LOCK_WITHDRAW.getCode());
        accountRollbackLockWithdrawTransaction.setOrder(rollbackLockWithdrawNumberCmd.getOrder());

        accountTransactionMapper.insert(accountRollbackLockWithdrawTransaction);

        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> releaseLockWithdrawNumber(AccountReleaseLockWithdrawNumberCmd accountReleaseLockWithdrawNumberCmd) {

        LambdaQueryWrapper<AccountTransaction> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(AccountTransaction::getWalletAddress, accountReleaseLockWithdrawNumberCmd.getWalletAddress());
        lambdaQueryWrapper.eq(AccountTransaction::getOrder, accountReleaseLockWithdrawNumberCmd.getOrder());
        lambdaQueryWrapper.eq(AccountTransaction::getTransactionType, AccountTransactionType.LOCK_WITHDRAW.getCode());
        lambdaQueryWrapper.orderByDesc(AccountTransaction::getId);
        lambdaQueryWrapper.last("LIMIT 1");

        AccountTransaction lockWithdrawTransaction = accountTransactionMapper.selectOne(lambdaQueryWrapper);

        Account account = accountMapper.selectById(lockWithdrawTransaction.getAccountId());
        if (account == null) {
            return SingleResponse.buildFailure("账户不存在");
        }

        String beforeWithdrawNumber = account.getWithdrawNumber();

        String beforeWithdrawLockNumber = account.getWithdrawLockNumber();

        account.setWithdrawNumber(String.valueOf(new BigDecimal(account.getWithdrawNumber()).add(new BigDecimal(lockWithdrawTransaction.getNumber()))));
        account.setWithdrawLockNumber(String.valueOf(new BigDecimal(account.getWithdrawLockNumber()).subtract(new BigDecimal(lockWithdrawTransaction.getNumber()))));

        account.setUpdateTime(System.currentTimeMillis());
        int updateCount = accountMapper.updateById(account);

        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("乐观锁异常");
        }


        AccountTransaction accountWithdrawTransaction = new AccountTransaction();
        accountWithdrawTransaction.setWalletAddress(lockWithdrawTransaction.getWalletAddress());
        accountWithdrawTransaction.setAccountId(account.getId());
        accountWithdrawTransaction.setBeforeNumber(beforeWithdrawNumber);
        accountWithdrawTransaction.setTransactionTime(System.currentTimeMillis());
        accountWithdrawTransaction.setNumber(lockWithdrawTransaction.getNumber());
        accountWithdrawTransaction.setAfterNumber(account.getWithdrawLockNumber());
        accountWithdrawTransaction.setAccountType(account.getType());
        accountWithdrawTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountWithdrawTransaction.setTransactionType(AccountTransactionType.WITHDRAW.getCode());
        accountWithdrawTransaction.setOrder(accountReleaseLockWithdrawNumberCmd.getOrder());
        accountTransactionMapper.insert(accountWithdrawTransaction);


        AccountTransaction accountLockWithdrawTransaction = new AccountTransaction();
        accountLockWithdrawTransaction.setWalletAddress(lockWithdrawTransaction.getWalletAddress());
        accountLockWithdrawTransaction.setAccountId(account.getId());
        accountLockWithdrawTransaction.setBeforeNumber(beforeWithdrawLockNumber);
        accountLockWithdrawTransaction.setTransactionTime(System.currentTimeMillis());
        accountLockWithdrawTransaction.setNumber(lockWithdrawTransaction.getNumber());
        accountLockWithdrawTransaction.setAfterNumber(account.getWithdrawLockNumber());
        accountLockWithdrawTransaction.setAccountType(account.getType());
        accountLockWithdrawTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountLockWithdrawTransaction.setTransactionType(AccountTransactionType.RELEASE_LOCK_WITHDRAW.getCode());
        accountLockWithdrawTransaction.setOrder(accountReleaseLockWithdrawNumberCmd.getOrder());
        accountTransactionMapper.insert(accountLockWithdrawTransaction);


        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> purchaseMinerProjectNumber(AccountDeductCmd accountDeductCmd) {

//        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(Account::getWalletAddress, accountDeductCmd.getWalletAddress());
//        queryWrapper.eq(Account::getType, accountDeductCmd.getAccountType());
//
//        Account account = accountMapper.selectOne(queryWrapper);
//        if (account == null) {
//            return SingleResponse.buildFailure(account.getType() + "账户不存在");
//        }

        Account account = getOrCreate(accountDeductCmd.getWalletAddress(), accountDeductCmd.getAccountType());

        BigDecimal balance = new BigDecimal(account.getNumber());

        BigDecimal deductNumber = new BigDecimal(accountDeductCmd.getNumber());

        if (balance.compareTo(deductNumber) < 0) {
            return SingleResponse.buildFailure(account.getType() + "账户余额不足");
        }


        String beforeNumber = account.getNumber();
        account.setNumber(String.valueOf(new BigDecimal(account.getNumber()).subtract(deductNumber)));
        account.setUpdateTime(System.currentTimeMillis());

        AccountTransaction accountTransaction = new AccountTransaction();
        accountTransaction.setWalletAddress(accountDeductCmd.getWalletAddress());
        accountTransaction.setAccountId(account.getId());
        accountTransaction.setBeforeNumber(beforeNumber);
        accountTransaction.setTransactionTime(System.currentTimeMillis());
        accountTransaction.setNumber(accountDeductCmd.getNumber());
        accountTransaction.setAfterNumber(account.getNumber());
        accountTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountTransaction.setTransactionType(AccountTransactionType.DEDUCT_NUMBER.getCode());
        accountTransaction.setOrder(accountDeductCmd.getOrder());
        accountTransaction.setAccountType(account.getType());

        accountTransactionMapper.insert(accountTransaction);

        int updateCount = accountMapper.updateById(account);
        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("扣除余额失败");
        }


        return SingleResponse.buildSuccess();

    }

    @Override
    public SingleResponse<Void> rollbackPurchaseMinerProjectNumber(AccountAddCmd accountAddCmd) {
        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Account::getWalletAddress, accountAddCmd.getWalletAddress());
        queryWrapper.eq(Account::getType, accountAddCmd.getAccountType());

        Account account = accountMapper.selectOne(queryWrapper);
        if (account == null) {
            return SingleResponse.buildFailure(account.getType() + "账户不存在");
        }

        BigDecimal addNumber = new BigDecimal(accountAddCmd.getNumber());

        String beforeNumber = account.getNumber();
        account.setNumber(String.valueOf(new BigDecimal(account.getNumber()).add(addNumber)));
        account.setUpdateTime(System.currentTimeMillis());

        AccountTransaction accountTransaction = new AccountTransaction();
        accountTransaction.setWalletAddress(accountAddCmd.getWalletAddress());
        accountTransaction.setAccountId(account.getId());
        accountTransaction.setBeforeNumber(beforeNumber);
        accountTransaction.setTransactionTime(System.currentTimeMillis());
        accountTransaction.setNumber(accountAddCmd.getNumber());
        accountTransaction.setAfterNumber(account.getNumber());
        accountTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountTransaction.setTransactionType(AccountTransactionType.ADD_NUMBER.getCode());
        accountTransaction.setOrder(accountAddCmd.getOrder());
        accountTransaction.setAccountType(account.getType());

        accountTransactionMapper.insert(accountTransaction);

        int updateCount = accountMapper.updateById(account);
        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("扣除余额失败");
        }


        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> withdrawServiceNumber(AccountWithdrawServiceCmd accountWithdrawServiceCmd) {
//        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(Account::getWalletAddress, accountWithdrawServiceCmd.getWalletAddress());
//        queryWrapper.eq(Account::getType, AccountType.ESG.getCode());
//
//        Account account = accountMapper.selectOne(queryWrapper);
//        if (account == null) {
//            return SingleResponse.buildFailure(account.getType() + "账户不存在");
//        }

        Account account = getOrCreate(accountWithdrawServiceCmd.getWalletAddress(),  AccountType.ESG.getCode());

        BigDecimal balance = new BigDecimal(account.getNumber());
        BigDecimal serviceNumber = new BigDecimal(accountWithdrawServiceCmd.getNumber());

        if (balance.compareTo(serviceNumber) < 0) {
            return SingleResponse.buildFailure(account.getType() + "账户余额不足");
        }


        String beforeNumber = account.getNumber();
        String beforeServiceLockNumber = account.getServiceLockNumber();

        account.setNumber(String.valueOf(new BigDecimal(account.getNumber()).subtract(serviceNumber)));
        account.setServiceLockNumber(String.valueOf(new BigDecimal(account.getServiceLockNumber()).add(new BigDecimal(accountWithdrawServiceCmd.getNumber()))));

        account.setUpdateTime(System.currentTimeMillis());

        AccountTransaction accountTransaction = new AccountTransaction();
        accountTransaction.setWalletAddress(accountWithdrawServiceCmd.getWalletAddress());
        accountTransaction.setAccountId(account.getId());
        accountTransaction.setBeforeNumber(beforeNumber);
        accountTransaction.setTransactionTime(System.currentTimeMillis());
        accountTransaction.setNumber(accountWithdrawServiceCmd.getNumber());
        accountTransaction.setAfterNumber(account.getNumber());
        accountTransaction.setAccountType(account.getType());
        accountTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountTransaction.setTransactionType(AccountTransactionType.DEDUCT_NUMBER.getCode());
        accountTransaction.setOrder(accountWithdrawServiceCmd.getOrder());
        accountTransactionMapper.insert(accountTransaction);

        AccountTransaction accountLockServiceTransaction = new AccountTransaction();
        accountLockServiceTransaction.setWalletAddress(accountWithdrawServiceCmd.getWalletAddress());
        accountLockServiceTransaction.setAccountId(account.getId());
        accountLockServiceTransaction.setBeforeNumber(beforeServiceLockNumber);
        accountLockServiceTransaction.setTransactionTime(System.currentTimeMillis());
        accountLockServiceTransaction.setNumber(accountWithdrawServiceCmd.getNumber());
        accountLockServiceTransaction.setAfterNumber(account.getServiceLockNumber());
        accountLockServiceTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountLockServiceTransaction.setTransactionType(AccountTransactionType.LOCK_WITHDRAW_SERVICE.getCode());
        accountLockServiceTransaction.setOrder(accountWithdrawServiceCmd.getOrder());
        accountLockServiceTransaction.setAccountType(account.getType());

        accountTransactionMapper.insert(accountLockServiceTransaction);

        int updateCount = accountMapper.updateById(account);
        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("扣除余额失败");
        }


        return SingleResponse.buildSuccess();

    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> rollbackLockWithdrawServiceNumber(RollbackLockWithdrawServiceCmd rollbackLockWithdrawServiceCmd) {
        LambdaQueryWrapper<AccountTransaction> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(AccountTransaction::getWalletAddress, rollbackLockWithdrawServiceCmd.getWalletAddress());
        lambdaQueryWrapper.eq(AccountTransaction::getOrder, rollbackLockWithdrawServiceCmd.getOrder());
        lambdaQueryWrapper.eq(AccountTransaction::getTransactionType, AccountTransactionType.LOCK_WITHDRAW_SERVICE.getCode());
        lambdaQueryWrapper.orderByDesc(AccountTransaction::getId);
        lambdaQueryWrapper.last("LIMIT 1");

        AccountTransaction lockServiceTransaction = accountTransactionMapper.selectOne(lambdaQueryWrapper);

        if (Objects.isNull(lockServiceTransaction)){
            return SingleResponse.buildSuccess();
        }

        Account account = accountMapper.selectById(lockServiceTransaction.getAccountId());


        String beforeServiceLockNumber = account.getServiceLockNumber();

        String beforeNumber = account.getNumber();

        account.setServiceLockNumber(String.valueOf(new BigDecimal(account.getServiceLockNumber()).subtract(new BigDecimal(lockServiceTransaction.getNumber()))));
        account.setNumber(String.valueOf(new BigDecimal(account.getNumber()).add(new BigDecimal(lockServiceTransaction.getNumber()))));
        account.setUpdateTime(System.currentTimeMillis());
        accountMapper.updateById(account);

        AccountTransaction accountTransaction = new AccountTransaction();
        accountTransaction.setWalletAddress(lockServiceTransaction.getWalletAddress());
        accountTransaction.setAccountId(account.getId());
        accountTransaction.setBeforeNumber(beforeNumber);
        accountTransaction.setTransactionTime(System.currentTimeMillis());
        accountTransaction.setNumber(lockServiceTransaction.getNumber());
        accountTransaction.setAfterNumber(account.getNumber());
        accountTransaction.setAccountType(account.getType());
        accountTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountTransaction.setTransactionType(AccountTransactionType.ADD_NUMBER.getCode());
        accountTransaction.setOrder(lockServiceTransaction.getOrder());

        accountTransactionMapper.insert(accountTransaction);

        AccountTransaction accountRollbackLockWithdrawTransaction = new AccountTransaction();
        accountRollbackLockWithdrawTransaction.setWalletAddress(lockServiceTransaction.getWalletAddress());
        accountRollbackLockWithdrawTransaction.setAccountId(account.getId());
        accountRollbackLockWithdrawTransaction.setBeforeNumber(beforeServiceLockNumber);
        accountRollbackLockWithdrawTransaction.setTransactionTime(System.currentTimeMillis());
        accountRollbackLockWithdrawTransaction.setNumber(lockServiceTransaction.getNumber());
        accountRollbackLockWithdrawTransaction.setAfterNumber(account.getSellLockNumber());
        accountRollbackLockWithdrawTransaction.setAccountType(account.getType());
        accountRollbackLockWithdrawTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountRollbackLockWithdrawTransaction.setTransactionType(AccountTransactionType.ROLLBACK_LOCK_WITHDRAW.getCode());
        accountRollbackLockWithdrawTransaction.setOrder(lockServiceTransaction.getOrder());

        accountTransactionMapper.insert(accountRollbackLockWithdrawTransaction);

        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> releaseLockWithdrawServiceNumber(AccountReleaseLockWithdrawServiceCmd accountReleaseLockWithdrawServiceCmd) {
        LambdaQueryWrapper<AccountTransaction> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(AccountTransaction::getWalletAddress, accountReleaseLockWithdrawServiceCmd.getWalletAddress());
        lambdaQueryWrapper.eq(AccountTransaction::getOrder, accountReleaseLockWithdrawServiceCmd.getOrder());
        lambdaQueryWrapper.eq(AccountTransaction::getTransactionType, AccountTransactionType.LOCK_WITHDRAW_SERVICE.getCode());
        lambdaQueryWrapper.orderByDesc(AccountTransaction::getId);
        lambdaQueryWrapper.last("LIMIT 1");

        AccountTransaction lockWithdrawServiceTransaction = accountTransactionMapper.selectOne(lambdaQueryWrapper);

        Account account = accountMapper.selectById(lockWithdrawServiceTransaction.getAccountId());
        if (account == null) {
            return SingleResponse.buildFailure("账户不存在");
        }

        String beforeServiceNumber = account.getServiceNumber();

        String beforeServiceLockNumber = account.getServiceLockNumber();

        account.setServiceNumber(String.valueOf(new BigDecimal(account.getServiceNumber()).add(new BigDecimal(lockWithdrawServiceTransaction.getNumber()))));
        account.setServiceLockNumber(String.valueOf(new BigDecimal(account.getServiceLockNumber()).subtract(new BigDecimal(lockWithdrawServiceTransaction.getNumber()))));

        account.setUpdateTime(System.currentTimeMillis());
        int updateCount = accountMapper.updateById(account);

        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("乐观锁异常");
        }


        AccountTransaction accountWithdrawServiceTransaction = new AccountTransaction();
        accountWithdrawServiceTransaction.setWalletAddress(lockWithdrawServiceTransaction.getWalletAddress());
        accountWithdrawServiceTransaction.setAccountId(account.getId());
        accountWithdrawServiceTransaction.setBeforeNumber(beforeServiceNumber);
        accountWithdrawServiceTransaction.setTransactionTime(System.currentTimeMillis());
        accountWithdrawServiceTransaction.setNumber(lockWithdrawServiceTransaction.getNumber());
        accountWithdrawServiceTransaction.setAfterNumber(account.getServiceNumber());
        accountWithdrawServiceTransaction.setAccountType(account.getType());
        accountWithdrawServiceTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountWithdrawServiceTransaction.setTransactionType(AccountTransactionType.WITHDRAW_SERVICE.getCode());
        accountWithdrawServiceTransaction.setOrder(lockWithdrawServiceTransaction.getOrder());
        accountTransactionMapper.insert(accountWithdrawServiceTransaction);


        AccountTransaction accountLockWithdrawServiceTransaction = new AccountTransaction();
        accountLockWithdrawServiceTransaction.setWalletAddress(lockWithdrawServiceTransaction.getWalletAddress());
        accountLockWithdrawServiceTransaction.setAccountId(account.getId());
        accountLockWithdrawServiceTransaction.setBeforeNumber(beforeServiceLockNumber);
        accountLockWithdrawServiceTransaction.setTransactionTime(System.currentTimeMillis());
        accountLockWithdrawServiceTransaction.setNumber(lockWithdrawServiceTransaction.getNumber());
        accountLockWithdrawServiceTransaction.setAfterNumber(account.getServiceLockNumber());
        accountLockWithdrawServiceTransaction.setAccountType(account.getType());
        accountLockWithdrawServiceTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountLockWithdrawServiceTransaction.setTransactionType(AccountTransactionType.RELEASE_LOCK_WITHDRAW_SERVICE.getCode());
        accountLockWithdrawServiceTransaction.setOrder(lockWithdrawServiceTransaction.getOrder());
        accountTransactionMapper.insert(accountLockWithdrawServiceTransaction);


        LambdaQueryWrapper<SystemConfig> systemConfigLambdaQueryWrapper = new LambdaQueryWrapper<>();
        systemConfigLambdaQueryWrapper.eq(SystemConfig::getName, SystemConfigEnum.SYSTEM_ADDRESS.getCode());

        SystemConfig systemConfig = systemConfigMapper.selectOne(systemConfigLambdaQueryWrapper);
        if (Objects.nonNull(systemConfig)){

            LambdaQueryWrapper<Account> accountLambdaQueryWrapper = new LambdaQueryWrapper<>();
            accountLambdaQueryWrapper.eq(Account::getWalletAddress,systemConfig.getValue());
            accountLambdaQueryWrapper.eq(Account::getType,AccountType.ESG.getCode());

            Account systemAccount = accountMapper.selectOne(accountLambdaQueryWrapper);
            if (Objects.nonNull(systemAccount)){

                String beforeSystemServiceNumber = systemAccount.getServiceNumber();

                systemAccount.setServiceNumber(String.valueOf(new BigDecimal(account.getServiceNumber())
                        .add(new BigDecimal(lockWithdrawServiceTransaction.getNumber()))));
                systemAccount.setUpdateTime(System.currentTimeMillis());
                int updateSystemCount = accountMapper.updateById(systemAccount);

                if (updateSystemCount == 0) {
                    throw new OptimisticLockingFailureException("乐观锁异常");
                }

                AccountTransaction accountServiceTransaction = new AccountTransaction();
                accountServiceTransaction.setWalletAddress(systemAccount.getWalletAddress());
                accountServiceTransaction.setAccountId(systemAccount.getId());
                accountServiceTransaction.setBeforeNumber(beforeSystemServiceNumber);
                accountServiceTransaction.setTransactionTime(System.currentTimeMillis());
                accountServiceTransaction.setNumber(lockWithdrawServiceTransaction.getNumber());
                accountServiceTransaction.setAfterNumber(systemAccount.getServiceNumber());
                accountServiceTransaction.setAccountType(systemAccount.getType());
                accountServiceTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
                accountServiceTransaction.setTransactionType(AccountTransactionType.WITHDRAW_SERVICE.getCode());
                accountServiceTransaction.setOrder(lockWithdrawServiceTransaction.getOrder());
                accountTransactionMapper.insert(accountServiceTransaction);

            }
        }

        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> rewardService(AccountDeductCmd accountDeductCmd) {

//        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(Account::getWalletAddress, accountDeductCmd.getWalletAddress());
//        queryWrapper.eq(Account::getType, accountDeductCmd.getAccountType());
//        queryWrapper.last("FOR UPDATE");
//
//        Account account = accountMapper.selectOne(queryWrapper);
//        if (account == null) {
//            return SingleResponse.buildFailure("账户不存在");
//        }

        Account account = getOrCreate(accountDeductCmd.getWalletAddress(), accountDeductCmd.getAccountType());

        BigDecimal balance = new BigDecimal(account.getNumber());

        if (balance.compareTo(new BigDecimal(accountDeductCmd.getNumber())) < 0) {
            return SingleResponse.buildFailure("账户余额不足");
        }

        String beforeNumber = account.getNumber();

        account.setNumber(String.valueOf(new BigDecimal(account.getNumber()).subtract(new BigDecimal(accountDeductCmd.getNumber()))));

        account.setUpdateTime(System.currentTimeMillis());
        int updateCount = accountMapper.updateById(account);

        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("乐观锁异常");
        }

        AccountTransaction accountTransaction = new AccountTransaction();
        accountTransaction.setWalletAddress(accountDeductCmd.getWalletAddress());
        accountTransaction.setAccountId(account.getId());
        accountTransaction.setBeforeNumber(beforeNumber);
        accountTransaction.setTransactionTime(System.currentTimeMillis());
        accountTransaction.setNumber(accountDeductCmd.getNumber());
        accountTransaction.setAfterNumber(account.getNumber());
        accountTransaction.setAccountType(account.getType());
        accountTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountTransaction.setTransactionType(AccountTransactionType.DEDUCT_REWARD_SERVICE.getCode());
        accountTransaction.setOrder(accountDeductCmd.getOrder());

        accountTransactionMapper.insert(accountTransaction);


        return SingleResponse.buildSuccess();
    }


    private Account getOrCreate(String walletAddress, String type){

        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Account::getWalletAddress, walletAddress);
        queryWrapper.eq(Account::getType, type);
        queryWrapper.last("FOR UPDATE");

        Account account = accountMapper.selectOne(queryWrapper);
        if (account == null) {

            account = new Account();
            account.setWalletAddress(walletAddress);
            account.setSellNumber("0");
            account.setSellLockNumber("0");
            account.setChargeNumber("0");
            account.setChargeLockNumber("0");
            account.setWithdrawNumber("0");
            account.setWithdrawLockNumber("0");
            account.setBuyNumber("0");
            account.setBuyLockNumber("0");
            account.setDynamicReward("0");
            account.setStaticReward("0");
            account.setNumber("0");
            account.setServiceNumber("0");
            account.setServiceLockNumber("0");
            account.setStaticRewardPrice("0");
            account.setDynamicRewardPrice("0");
            account.setType(type);
            account.setCreateTime(System.currentTimeMillis());
            account.setUpdateTime(System.currentTimeMillis());

            accountMapper.insert(account);
        }

        return account;
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> transferEco(AccountTransferCmd accountTransferCmd) {
        // 参数验证（在事务外进行，避免无效参数触发回滚）
        if (accountTransferCmd.getFromWalletAddress() == null || accountTransferCmd.getFromWalletAddress().trim().isEmpty()) {
            return SingleResponse.buildFailure("转出钱包地址不能为空");
        }
        if (accountTransferCmd.getToWalletAddress() == null || accountTransferCmd.getToWalletAddress().trim().isEmpty()) {
            return SingleResponse.buildFailure("转入钱包地址不能为空");
        }
        if (accountTransferCmd.getAmount() == null || accountTransferCmd.getAmount().trim().isEmpty()) {
            return SingleResponse.buildFailure("转账金额不能为空");
        }
        if (accountTransferCmd.getFromWalletAddress().equals(accountTransferCmd.getToWalletAddress())) {
            return SingleResponse.buildFailure("转出和转入钱包地址不能相同");
        }

        BigDecimal transferAmount;
        try {
            transferAmount = new BigDecimal(accountTransferCmd.getAmount());
            if (transferAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return SingleResponse.buildFailure("转账金额必须大于0");
            }
        } catch (NumberFormatException e) {
            return SingleResponse.buildFailure("转账金额格式错误");
        }

        try {

            // 获取或创建转出账户（ECO类型）
            Account fromAccount = getOrCreate(accountTransferCmd.getFromWalletAddress(), AccountType.ECO.getCode());
            
            // 检查转出账户余额
            BigDecimal fromBalance = new BigDecimal(fromAccount.getNumber());
            if (fromBalance.compareTo(transferAmount) < 0) {
                return SingleResponse.buildFailure("转出账户ECO余额不足");
            }

            // 获取或创建转入账户（ECO类型）
            Account toAccount = getOrCreate(accountTransferCmd.getToWalletAddress(), AccountType.ECO.getCode());

            // 生成订单号
            String order = "TR" + System.currentTimeMillis();

            // 执行转账操作
            // 1. 扣除转出账户余额
            String fromBeforeNumber = fromAccount.getNumber();
            fromAccount.setNumber(String.valueOf(fromBalance.subtract(transferAmount)));
            fromAccount.setUpdateTime(System.currentTimeMillis());
            
            int fromUpdateCount = accountMapper.updateById(fromAccount);
            if (fromUpdateCount == 0) {
                throw new OptimisticLockingFailureException("更新转出账户失败");
            }

            // 2. 增加转入账户余额
            String toBeforeNumber = toAccount.getNumber();
            BigDecimal toBalance = new BigDecimal(toAccount.getNumber());
            toAccount.setNumber(String.valueOf(toBalance.add(transferAmount)));
            toAccount.setUpdateTime(System.currentTimeMillis());
            
            int toUpdateCount = accountMapper.updateById(toAccount);
            if (toUpdateCount == 0) {
                throw new OptimisticLockingFailureException("更新转入账户失败");
            }

            // 3. 记录转出交易
            AccountTransaction fromTransaction = new AccountTransaction();
            fromTransaction.setWalletAddress(accountTransferCmd.getFromWalletAddress());
            fromTransaction.setAccountId(fromAccount.getId());
            fromTransaction.setBeforeNumber(fromBeforeNumber);
            fromTransaction.setTransactionTime(System.currentTimeMillis());
            fromTransaction.setNumber(accountTransferCmd.getAmount());
            fromTransaction.setAfterNumber(fromAccount.getNumber());
            fromTransaction.setAccountType(AccountType.ECO.getCode());
            fromTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
            fromTransaction.setTransactionType(AccountTransactionType.TRANSFER_OUT.getCode());
            fromTransaction.setOrder(order);
            fromTransaction.setRemark(accountTransferCmd.getRemark());
            accountTransactionMapper.insert(fromTransaction);

            // 4. 记录转入交易
            AccountTransaction toTransaction = new AccountTransaction();
            toTransaction.setWalletAddress(accountTransferCmd.getToWalletAddress());
            toTransaction.setAccountId(toAccount.getId());
            toTransaction.setBeforeNumber(toBeforeNumber);
            toTransaction.setTransactionTime(System.currentTimeMillis());
            toTransaction.setNumber(accountTransferCmd.getAmount());
            toTransaction.setAfterNumber(toAccount.getNumber());
            toTransaction.setAccountType(AccountType.ECO.getCode());
            toTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
            toTransaction.setTransactionType(AccountTransactionType.TRANSFER_IN.getCode());
            toTransaction.setOrder(order);
            toTransaction.setRemark(accountTransferCmd.getRemark());
            accountTransactionMapper.insert(toTransaction);

            return SingleResponse.buildSuccess();

        } catch (Exception e) {
            e.printStackTrace();
            // 重新抛出异常以触发事务回滚
            throw new RuntimeException("转账失败: " + e.getMessage(), e);
        }
    }
}
