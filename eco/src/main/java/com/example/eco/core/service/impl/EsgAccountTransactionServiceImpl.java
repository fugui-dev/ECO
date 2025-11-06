package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.cmd.AccountTransactionPageQry;
import com.example.eco.bean.dto.AccountTransactionDTO;
import com.example.eco.bean.dto.EsgAccountTransactionDTO;
import com.example.eco.common.AccountTransactionStatusEnum;
import com.example.eco.common.AccountTransactionType;
import com.example.eco.common.AccountType;
import com.example.eco.core.service.AccountTransactionService;
import com.example.eco.core.service.EsgAccountTransactionService;
import com.example.eco.model.entity.AccountTransaction;
import com.example.eco.model.entity.EsgAccountTransaction;
import com.example.eco.model.mapper.AccountTransactionMapper;
import com.example.eco.model.mapper.EsgAccountTransactionMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class EsgAccountTransactionServiceImpl implements EsgAccountTransactionService {

    @Resource
    private EsgAccountTransactionMapper esgAccountTransactionMapper;

    @Override
    public MultiResponse<EsgAccountTransactionDTO> page(AccountTransactionPageQry accountTransactionPageQry) {

        LambdaQueryWrapper<EsgAccountTransaction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StringUtils.hasLength(accountTransactionPageQry.getWalletAddress()), EsgAccountTransaction::getWalletAddress, accountTransactionPageQry.getWalletAddress());
        queryWrapper.eq(StringUtils.hasLength(accountTransactionPageQry.getTransactionStatus()), EsgAccountTransaction::getStatus, accountTransactionPageQry.getTransactionStatus());
        queryWrapper.eq(StringUtils.hasLength(accountTransactionPageQry.getOrder()), EsgAccountTransaction::getOrder, accountTransactionPageQry.getOrder());
        queryWrapper.eq(StringUtils.hasLength(accountTransactionPageQry.getHash()), EsgAccountTransaction::getHash, accountTransactionPageQry.getHash());

        queryWrapper.eq(EsgAccountTransaction::getTransactionType, AccountTransactionType.ESG_NFT_STATIC_REWARD.getCode());

        queryWrapper.orderByDesc(EsgAccountTransaction::getTransactionTime);

        Page<EsgAccountTransaction> accountTransactionPage = esgAccountTransactionMapper.selectPage(Page.of(accountTransactionPageQry.getPageNum(), accountTransactionPageQry.getPageSize()), queryWrapper);

        if(CollectionUtils.isEmpty(accountTransactionPage.getRecords())){
            return MultiResponse.buildSuccess();
        }

        List<EsgAccountTransactionDTO> accountTransactionDTOList = new ArrayList<>();
        for(EsgAccountTransaction accountTransaction : accountTransactionPage.getRecords()){
            EsgAccountTransactionDTO accountTransactionDTO = new EsgAccountTransactionDTO();
            BeanUtils.copyProperties(accountTransaction, accountTransactionDTO);
            accountTransactionDTO.setTransactionTypeName(AccountTransactionType.of(accountTransaction.getTransactionType()).getName());
            accountTransactionDTO.setStatusName(AccountTransactionStatusEnum.of(accountTransaction.getStatus()).getName());
            accountTransactionDTOList.add(accountTransactionDTO);
        }
        return MultiResponse.of(accountTransactionDTOList,(int)accountTransactionPage.getTotal());
    }

}
