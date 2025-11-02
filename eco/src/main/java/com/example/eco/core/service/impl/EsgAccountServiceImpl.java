package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.bean.dto.EsgAccountDTO;
import com.example.eco.common.AccountTransactionStatusEnum;
import com.example.eco.common.AccountTransactionType;
import com.example.eco.core.service.EsgAccountService;
import com.example.eco.model.entity.*;
import com.example.eco.model.mapper.EsgAccountMapper;
import com.example.eco.model.mapper.EsgAccountTransactionMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Objects;

@Service
public class EsgAccountServiceImpl implements EsgAccountService {

    @Resource
    private EsgAccountMapper esgAccountMapper;
    @Resource
    private EsgAccountTransactionMapper esgAccountTransactionMapper;


    @Override
    public SingleResponse<EsgAccountDTO> getAccount(EsgAccountQry esgAccountQry) {

        LambdaQueryWrapper<EsgAccount> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EsgAccount::getWalletAddress, esgAccountQry.getWalletAddress());

        EsgAccount esgAccount = esgAccountMapper.selectOne(queryWrapper);

        if (Objects.isNull(esgAccount)) {

            esgAccount = new EsgAccount();
            esgAccount.setWalletAddress(esgAccountQry.getWalletAddress());
            esgAccount.setNumber("0");
            esgAccount.setStaticReward("0");
            esgAccount.setChargeNumber("0");
            esgAccount.setChargeLockNumber("0");
            esgAccount.setCreateTime(System.currentTimeMillis());
            esgAccount.setUpdateTime(System.currentTimeMillis());
            esgAccountMapper.insert(esgAccount);

        }

        EsgAccountDTO esgAccountDTO = new EsgAccountDTO();
        BeanUtils.copyProperties(esgAccount, esgAccountDTO);

        return SingleResponse.of(esgAccountDTO);
    }


    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> addStaticNumber(AccountStaticNumberCmd accountStaticNumberCmd) {

        EsgAccount account = getOrCreate(accountStaticNumberCmd.getWalletAddress());


        String beforeStaticReward = account.getStaticReward();

        account.setStaticReward(String.valueOf(new BigDecimal(account.getStaticReward()).add(new BigDecimal(accountStaticNumberCmd.getNumber()))));

        account.setUpdateTime(System.currentTimeMillis());

        int updateCount = esgAccountMapper.updateById(account);

        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("乐观锁异常");
        }


        EsgAccountTransaction accountStaticTransaction = new EsgAccountTransaction();
        accountStaticTransaction.setWalletAddress(accountStaticNumberCmd.getWalletAddress());
        accountStaticTransaction.setAccountId(account.getId());
        accountStaticTransaction.setBeforeNumber(beforeStaticReward);
        accountStaticTransaction.setTransactionTime(System.currentTimeMillis());
        accountStaticTransaction.setNumber(accountStaticNumberCmd.getNumber());
        accountStaticTransaction.setAfterNumber(account.getStaticReward());
        accountStaticTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountStaticTransaction.setTransactionType(AccountTransactionType.ESG_NFT_STATIC_REWARD.getCode());
        accountStaticTransaction.setOrder(accountStaticNumberCmd.getOrder());
        esgAccountTransactionMapper.insert(accountStaticTransaction);

        return SingleResponse.buildSuccess();
    }


    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> chargeNumber(AccountChargeNumberCmd accountChargeNumberCmd) {

        EsgAccount account = getOrCreate(accountChargeNumberCmd.getWalletAddress());

        String beforeChargeLockNumber = account.getChargeLockNumber();

        account.setChargeLockNumber(String.valueOf(new BigDecimal(account.getChargeLockNumber()).add(new BigDecimal(accountChargeNumberCmd.getNumber()))));

        account.setUpdateTime(System.currentTimeMillis());
        int updateCount = esgAccountMapper.updateById(account);

        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("乐观锁异常");
        }

        EsgAccountTransaction accountLockChargeTransaction = new EsgAccountTransaction();
        accountLockChargeTransaction.setWalletAddress(accountChargeNumberCmd.getWalletAddress());
        accountLockChargeTransaction.setAccountId(account.getId());
        accountLockChargeTransaction.setBeforeNumber(beforeChargeLockNumber);
        accountLockChargeTransaction.setTransactionTime(System.currentTimeMillis());
        accountLockChargeTransaction.setNumber(accountChargeNumberCmd.getNumber());
        accountLockChargeTransaction.setAfterNumber(account.getChargeLockNumber());
        accountLockChargeTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountLockChargeTransaction.setTransactionType(AccountTransactionType.LOCK_CHARGE.getCode());
        accountLockChargeTransaction.setOrder(accountChargeNumberCmd.getOrder());
        accountLockChargeTransaction.setHash(accountChargeNumberCmd.getHash());

        esgAccountTransactionMapper.insert(accountLockChargeTransaction);

        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> releaseLockChargeNumber(AccountLockChargeNumberCmd accountLockChargeNumberCmd) {

        LambdaQueryWrapper<EsgAccountTransaction> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(EsgAccountTransaction::getWalletAddress, accountLockChargeNumberCmd.getWalletAddress());
        lambdaQueryWrapper.eq(EsgAccountTransaction::getOrder, accountLockChargeNumberCmd.getOrder());
        lambdaQueryWrapper.eq(EsgAccountTransaction::getHash, accountLockChargeNumberCmd.getHash());
        lambdaQueryWrapper.eq(EsgAccountTransaction::getTransactionType, AccountTransactionType.LOCK_CHARGE.getCode());
        lambdaQueryWrapper.orderByDesc(EsgAccountTransaction::getId);
        lambdaQueryWrapper.last("LIMIT 1");

        EsgAccountTransaction lockChargeTransaction = esgAccountTransactionMapper.selectOne(lambdaQueryWrapper);

        EsgAccount account = esgAccountMapper.selectById(lockChargeTransaction.getAccountId());

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
        int updateCount = esgAccountMapper.updateById(account);

        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("乐观锁异常");
        }

        EsgAccountTransaction accountTransaction = new EsgAccountTransaction();
        accountTransaction.setWalletAddress(accountLockChargeNumberCmd.getWalletAddress());
        accountTransaction.setAccountId(account.getId());
        accountTransaction.setBeforeNumber(beforeNumber);
        accountTransaction.setTransactionTime(System.currentTimeMillis());
        accountTransaction.setNumber(lockChargeTransaction.getNumber());
        accountTransaction.setAfterNumber(account.getNumber());
        accountTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountTransaction.setTransactionType(AccountTransactionType.ADD_NUMBER.getCode());
        accountTransaction.setOrder(lockChargeTransaction.getOrder());
        accountTransaction.setHash(accountLockChargeNumberCmd.getHash());
        esgAccountTransactionMapper.insert(accountTransaction);

        EsgAccountTransaction accountChargeTransaction = new EsgAccountTransaction();
        accountChargeTransaction.setWalletAddress(accountLockChargeNumberCmd.getWalletAddress());
        accountChargeTransaction.setAccountId(account.getId());
        accountChargeTransaction.setBeforeNumber(beforeChargeNumber);
        accountChargeTransaction.setTransactionTime(System.currentTimeMillis());
        accountChargeTransaction.setNumber(lockChargeTransaction.getNumber());
        accountChargeTransaction.setAfterNumber(account.getChargeNumber());
        accountChargeTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountChargeTransaction.setTransactionType(AccountTransactionType.CHARGE.getCode());
        accountChargeTransaction.setOrder(lockChargeTransaction.getOrder());
        accountChargeTransaction.setHash(accountLockChargeNumberCmd.getHash());
        esgAccountTransactionMapper.insert(accountChargeTransaction);


        EsgAccountTransaction accountReleaseChargeTransaction = new EsgAccountTransaction();
        accountReleaseChargeTransaction.setWalletAddress(accountLockChargeNumberCmd.getWalletAddress());
        accountReleaseChargeTransaction.setAccountId(account.getId());
        accountReleaseChargeTransaction.setBeforeNumber(beforeChargeLockNumber);
        accountReleaseChargeTransaction.setTransactionTime(System.currentTimeMillis());
        accountReleaseChargeTransaction.setNumber(lockChargeTransaction.getNumber());
        accountReleaseChargeTransaction.setAfterNumber(account.getChargeLockNumber());
        accountReleaseChargeTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountReleaseChargeTransaction.setTransactionType(AccountTransactionType.RELEASE_LOCK_CHARGE.getCode());
        accountReleaseChargeTransaction.setHash(accountLockChargeNumberCmd.getHash());
        accountReleaseChargeTransaction.setOrder(lockChargeTransaction.getOrder());
        esgAccountTransactionMapper.insert(accountReleaseChargeTransaction);


        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> rollbackLockChargeNumber(RollbackLockChargeNumberCmd rollbackLockChargeNumberCmd) {

        LambdaQueryWrapper<EsgAccountTransaction> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(EsgAccountTransaction::getWalletAddress, rollbackLockChargeNumberCmd.getWalletAddress());
        lambdaQueryWrapper.eq(EsgAccountTransaction::getOrder, rollbackLockChargeNumberCmd.getOrder());
        lambdaQueryWrapper.eq(EsgAccountTransaction::getHash, rollbackLockChargeNumberCmd.getHash());
        lambdaQueryWrapper.eq(EsgAccountTransaction::getTransactionType, AccountTransactionType.LOCK_CHARGE.getCode());
        lambdaQueryWrapper.orderByDesc(EsgAccountTransaction::getId);
        lambdaQueryWrapper.last("LIMIT 1");

        EsgAccountTransaction lockChargeTransaction = esgAccountTransactionMapper.selectOne(lambdaQueryWrapper);

        EsgAccount account = esgAccountMapper.selectById(lockChargeTransaction.getAccountId());
        if (account == null) {
            return SingleResponse.buildFailure("账户不存在");
        }

        String beforeChargeLockNumber = account.getChargeLockNumber();

        account.setChargeLockNumber(String.valueOf(new BigDecimal(account.getChargeLockNumber()).subtract(new BigDecimal(lockChargeTransaction.getNumber()))));
        account.setUpdateTime(System.currentTimeMillis());

        int updateCount = esgAccountMapper.updateById(account);

        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("乐观锁异常");
        }

        EsgAccountTransaction accountRollbackLockChargeTransaction = new EsgAccountTransaction();
        accountRollbackLockChargeTransaction.setWalletAddress(rollbackLockChargeNumberCmd.getWalletAddress());
        accountRollbackLockChargeTransaction.setAccountId(account.getId());
        accountRollbackLockChargeTransaction.setBeforeNumber(beforeChargeLockNumber);
        accountRollbackLockChargeTransaction.setTransactionTime(System.currentTimeMillis());
        accountRollbackLockChargeTransaction.setNumber(lockChargeTransaction.getNumber());
        accountRollbackLockChargeTransaction.setAfterNumber(account.getChargeLockNumber());
        accountRollbackLockChargeTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountRollbackLockChargeTransaction.setTransactionType(AccountTransactionType.ROLLBACK_LOCK_CHARGE.getCode());
        accountRollbackLockChargeTransaction.setOrder(rollbackLockChargeNumberCmd.getOrder());

        esgAccountTransactionMapper.insert(accountRollbackLockChargeTransaction);

        return SingleResponse.buildSuccess();
    }

    @Override
    public SingleResponse<Void> purchaseMinerProjectNumber(AccountDeductCmd accountDeductCmd) {

        EsgAccount account = getOrCreate(accountDeductCmd.getWalletAddress());

        BigDecimal balance = new BigDecimal(account.getNumber());

        BigDecimal deductNumber = new BigDecimal(accountDeductCmd.getNumber());

        if (balance.compareTo(deductNumber) < 0) {
            return SingleResponse.buildFailure("账户余额不足");
        }


        String beforeNumber = account.getNumber();

        account.setNumber(String.valueOf(new BigDecimal(account.getNumber()).subtract(deductNumber)));

        account.setUpdateTime(System.currentTimeMillis());

        EsgAccountTransaction accountTransaction = new EsgAccountTransaction();
        accountTransaction.setWalletAddress(accountDeductCmd.getWalletAddress());
        accountTransaction.setAccountId(account.getId());
        accountTransaction.setBeforeNumber(beforeNumber);
        accountTransaction.setTransactionTime(System.currentTimeMillis());
        accountTransaction.setNumber(accountDeductCmd.getNumber());
        accountTransaction.setAfterNumber(account.getNumber());
        accountTransaction.setStatus(AccountTransactionStatusEnum.SUCCESS.getCode());
        accountTransaction.setTransactionType(AccountTransactionType.DEDUCT_NUMBER.getCode());
        accountTransaction.setOrder(accountDeductCmd.getOrder());

        esgAccountTransactionMapper.insert(accountTransaction);

        int updateCount = esgAccountMapper.updateById(account);
        if (updateCount == 0) {
            throw new OptimisticLockingFailureException("扣除余额失败");
        }


        return SingleResponse.buildSuccess();
    }


    public EsgAccount getOrCreate(String walletAddress){

        LambdaQueryWrapper<EsgAccount> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EsgAccount::getWalletAddress, walletAddress);
        queryWrapper.last("FOR UPDATE");

        EsgAccount account = esgAccountMapper.selectOne(queryWrapper);
        if (account == null) {

            account = new EsgAccount();
            account.setWalletAddress(walletAddress);
            account.setChargeNumber("0");
            account.setChargeLockNumber("0");
            account.setStaticReward("0");
            account.setNumber("0");
            account.setCreateTime(System.currentTimeMillis());
            account.setUpdateTime(System.currentTimeMillis());

            esgAccountMapper.insert(account);
        }

        return account;
    }
}
