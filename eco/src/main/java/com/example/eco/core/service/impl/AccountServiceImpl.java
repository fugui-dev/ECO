package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.AccountCreateCmd;
import com.example.eco.common.AccountType;
import com.example.eco.core.service.AccountService;
import com.example.eco.model.entity.Account;
import com.example.eco.model.mapper.AccountMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class AccountServiceImpl implements AccountService {

    @Resource
    private AccountMapper accountMapper;

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
        existingEcoAccount.setBuyNumber("0");
        existingEcoAccount.setNumber("0");
        existingEcoAccount.setType(AccountType.ECO.getCode());
        existingEcoAccount.setDynamicReward("0");
        existingEcoAccount.setLockNumber("0");
        existingEcoAccount.setStaticReward("0");
        existingEcoAccount.setWithdrawNumber("0");
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
        existingEsgAccount.setBuyNumber("0");
        existingEsgAccount.setNumber("0");
        existingEsgAccount.setType(AccountType.ESG.getCode());
        existingEsgAccount.setDynamicReward("0");
        existingEsgAccount.setLockNumber("0");
        existingEsgAccount.setStaticReward("0");
        existingEsgAccount.setWithdrawNumber("0");
        existingEsgAccount.setCreateTime(System.currentTimeMillis());
        existingEsgAccount.setUpdateTime(System.currentTimeMillis());
        accountMapper.insert(existingEsgAccount);

        return SingleResponse.buildSuccess();
    }
}
