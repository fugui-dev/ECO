package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.cmd.AccountTransactionPageQry;
import com.example.eco.bean.dto.AccountTransactionDTO;
import com.example.eco.common.AccountTransactionStatusEnum;
import com.example.eco.common.AccountTransactionType;
import com.example.eco.common.AccountType;
import com.example.eco.core.service.AccountTransactionService;
import com.example.eco.model.entity.AccountTransaction;
import com.example.eco.model.mapper.AccountTransactionMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class AccountTransactionServiceImpl implements AccountTransactionService {

    @Resource
    private AccountTransactionMapper accountTransactionMapper;

    @Override
    public MultiResponse<AccountTransactionDTO> page(AccountTransactionPageQry accountTransactionPageQry) {

        LambdaQueryWrapper<AccountTransaction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StringUtils.hasLength(accountTransactionPageQry.getWalletAddress()), AccountTransaction::getWalletAddress, accountTransactionPageQry.getWalletAddress());
        queryWrapper.eq(StringUtils.hasLength(accountTransactionPageQry.getAccountType()), AccountTransaction::getAccountType, accountTransactionPageQry.getAccountType());
        queryWrapper.eq(StringUtils.hasLength(accountTransactionPageQry.getTransactionType()), AccountTransaction::getTransactionType, accountTransactionPageQry.getTransactionType());
        queryWrapper.eq(StringUtils.hasLength(accountTransactionPageQry.getTransactionStatus()), AccountTransaction::getStatus, accountTransactionPageQry.getTransactionStatus());
        queryWrapper.eq(Objects.nonNull(accountTransactionPageQry.getOrderId()), AccountTransaction::getOrderId, accountTransactionPageQry.getOrderId());
        queryWrapper.orderByDesc(AccountTransaction::getTransactionTime);

        Page<AccountTransaction> accountTransactionPage = accountTransactionMapper.selectPage(Page.of(accountTransactionPageQry.getPageNum(), accountTransactionPageQry.getPageSize()), queryWrapper);

        if(CollectionUtils.isEmpty(accountTransactionPage.getRecords())){
            return MultiResponse.buildSuccess();
        }

        List<AccountTransactionDTO> accountTransactionDTOList = new ArrayList<>();
        for(AccountTransaction accountTransaction : accountTransactionPage.getRecords()){
            AccountTransactionDTO accountTransactionDTO = new AccountTransactionDTO();
            BeanUtils.copyProperties(accountTransaction, accountTransactionDTO);
            accountTransactionDTO.setAccountTypeName(AccountType.of(accountTransaction.getAccountType()).getName());
            accountTransactionDTO.setTransactionTypeName(AccountTransactionType.of(accountTransaction.getTransactionType()).getName());
            accountTransactionDTO.setStatusName(AccountTransactionStatusEnum.of(accountTransaction.getStatus()).getName());
            accountTransactionDTOList.add(accountTransactionDTO);
        }
        return MultiResponse.of(accountTransactionDTOList,(int)accountTransactionPage.getTotal());
    }
}
