package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.PendOrderCreateCmd;
import com.example.eco.core.service.PendOrderService;
import com.example.eco.model.entity.Account;
import com.example.eco.model.mapper.AccountMapper;
import com.example.eco.model.mapper.PendOrderMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;

@Service
public class PendOrderServiceImpl implements PendOrderService {

    @Resource
    private PendOrderMapper pendOrderMapper;
    @Resource
    private AccountMapper accountMapper;


    @Override
    public SingleResponse<Void> createPendOrder(PendOrderCreateCmd pendOrderCreateCmd) {

        LambdaQueryWrapper<Account> accountLambdaQueryWrapper = new LambdaQueryWrapper<>();
        accountLambdaQueryWrapper.eq(Account::getWalletAddress, pendOrderCreateCmd.getWalletAddress());
        accountLambdaQueryWrapper.eq(Account::getType,pendOrderCreateCmd.getType());

        Account account = accountMapper.selectOne(accountLambdaQueryWrapper);
        if (account == null) {
            return SingleResponse.buildFailure("账户不存在");
        }

        BigDecimal number = new BigDecimal(pendOrderCreateCmd.getNumber());

        if (new BigDecimal(account.getNumber()).compareTo(number) < 0) {
            return SingleResponse.buildFailure("余额不足");
        }



        return null;
    }
}
