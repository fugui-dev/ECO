package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.bean.dto.ChargeOrderDTO;
import com.example.eco.common.AccountType;
import com.example.eco.common.ChargeOrderStatus;
import com.example.eco.core.service.AccountService;
import com.example.eco.core.service.ChargeOrderService;
import com.example.eco.model.entity.ChargeOrder;
import com.example.eco.model.entity.EtherScanAccountTransaction;
import com.example.eco.model.mapper.ChargeOrderMapper;
import com.example.eco.model.mapper.EtherScanAccountTransactionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class ChargeOrderServiceImpl implements ChargeOrderService {

    @Resource
    private ChargeOrderMapper chargeOrderMapper;

    @Resource
    private AccountService accountService;

    @Resource
    private EtherScanAccountTransactionMapper etherScanAccountTransactionMapper;


    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> create(ChargeOrderCreateCmd chargeOrderCreateCmd) {

        String order = "CO" + System.currentTimeMillis();

        AccountChargeNumberCmd accountChargeNumberCmd = new AccountChargeNumberCmd();
        accountChargeNumberCmd.setWalletAddress(chargeOrderCreateCmd.getWalletAddress());
        accountChargeNumberCmd.setType(chargeOrderCreateCmd.getType());
        accountChargeNumberCmd.setNumber(chargeOrderCreateCmd.getNumber());
        accountChargeNumberCmd.setOrder(order);
        accountChargeNumberCmd.setHash(chargeOrderCreateCmd.getHash());

        try {
            SingleResponse<Void> response = accountService.chargeNumber(accountChargeNumberCmd);
            if (!response.isSuccess()) {
                return response;
            }
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("创建充值订单异常");
        }

        ChargeOrder chargeOrder = new ChargeOrder();
        chargeOrder.setOrder(order);
        chargeOrder.setWalletAddress(chargeOrderCreateCmd.getWalletAddress());
        chargeOrder.setNumber(chargeOrderCreateCmd.getNumber());
        chargeOrder.setStatus(ChargeOrderStatus.PENDING.getCode());
        chargeOrder.setHash(chargeOrderCreateCmd.getHash());
        chargeOrder.setType(chargeOrderCreateCmd.getType());
        chargeOrder.setPrice(chargeOrderCreateCmd.getPrice());
        chargeOrder.setTotalPrice(chargeOrderCreateCmd.getTotalPrice());
        chargeOrder.setCreateTime(System.currentTimeMillis());

        chargeOrderMapper.insert(chargeOrder);

        return SingleResponse.buildSuccess();
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> update(ChargeOrderUpdateCmd chargeOrderUpdateCmd) {

        LambdaQueryWrapper<ChargeOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChargeOrder::getHash, chargeOrderUpdateCmd.getHash());

        ChargeOrder chargeOrder = chargeOrderMapper.selectOne(queryWrapper);
        if (Objects.isNull(chargeOrder)){
            return SingleResponse.buildFailure("充值订单不存在");
        }

        if (!ChargeOrderStatus.PENDING.getCode().equals(chargeOrder.getStatus())) {
            return SingleResponse.buildFailure("充值订单状态异常");
        }

        if (ChargeOrderStatus.SUCCESS.getCode().equals(chargeOrderUpdateCmd.getStatus())) {
            chargeOrder.setStatus(ChargeOrderStatus.SUCCESS.getCode());
            chargeOrder.setFinishTime(System.currentTimeMillis());

            AccountLockChargeNumberCmd accountLockChargeNumberCmd = new AccountLockChargeNumberCmd();
            accountLockChargeNumberCmd.setWalletAddress(chargeOrder.getWalletAddress());
            accountLockChargeNumberCmd.setOrder(chargeOrder.getOrder());
            accountLockChargeNumberCmd.setHash(chargeOrder.getHash());

            try {
                SingleResponse<Void> response = accountService.releaseLockChargeNumber(accountLockChargeNumberCmd);

                if (!response.isSuccess()) {
                    return response;
                }
            }catch (Exception e){
                e.printStackTrace();
                throw new RuntimeException("更新充值订单失败");
            }
        }else {

            chargeOrder.setStatus(ChargeOrderStatus.FAILED.getCode());
            chargeOrder.setFinishTime(System.currentTimeMillis());

            RollbackLockChargeNumberCmd rollbackLockChargeNumberCmd = new RollbackLockChargeNumberCmd();
            rollbackLockChargeNumberCmd.setWalletAddress(chargeOrder.getWalletAddress());
            rollbackLockChargeNumberCmd.setOrder(chargeOrder.getOrder());
            rollbackLockChargeNumberCmd.setHash(chargeOrder.getHash());

            try {
                SingleResponse<Void> response = accountService.rollbackLockChargeNumber(rollbackLockChargeNumberCmd);

                if (!response.isSuccess()) {
                    log.info("更新充值订单失败{}",response.getErrMessage());
                    throw new RuntimeException("更新充值订单失败");
                }
            }catch (Exception e){
                e.printStackTrace();
                throw new RuntimeException("更新充值订单失败");
            }
        }

        chargeOrderMapper.updateById(chargeOrder);
        return SingleResponse.buildSuccess();
    }

    @Override
    public MultiResponse<ChargeOrderDTO> page(ChargeOrderPageQry chargeOrderPageQry) {

        LambdaQueryWrapper<ChargeOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StringUtils.isNotEmpty(chargeOrderPageQry.getWalletAddress()), ChargeOrder::getWalletAddress, chargeOrderPageQry.getWalletAddress());
        queryWrapper.eq(StringUtils.isNotEmpty(chargeOrderPageQry.getType()), ChargeOrder::getType, chargeOrderPageQry.getType());
        queryWrapper.eq(StringUtils.isNotEmpty(chargeOrderPageQry.getStatus()), ChargeOrder::getStatus, chargeOrderPageQry.getStatus());
        queryWrapper.eq(StringUtils.isNotEmpty(chargeOrderPageQry.getOrder()), ChargeOrder::getOrder, chargeOrderPageQry.getOrder());

        Page<ChargeOrder> chargeOrderPage = chargeOrderMapper.selectPage(Page.of(chargeOrderPageQry.getPageNum(), chargeOrderPageQry.getPageSize()), queryWrapper);

        List<ChargeOrderDTO> chargeOrderList = new ArrayList<>();

        for (ChargeOrder chargeOrder : chargeOrderPage.getRecords()) {
            ChargeOrderDTO chargeOrderDTO = new ChargeOrderDTO();
            BeanUtils.copyProperties(chargeOrder, chargeOrderDTO);

            chargeOrderDTO.setTypeName(AccountType.of(chargeOrder.getType()).getName());
            chargeOrderDTO.setStatusName(ChargeOrderStatus.of(chargeOrder.getStatus()).getName());
            chargeOrderList.add(chargeOrderDTO);
        }
        return MultiResponse.of(chargeOrderList, (int) chargeOrderPage.getTotal());
    }

    @Override
    public SingleResponse<Void> checkChargeOrder() {

        LambdaQueryWrapper<ChargeOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChargeOrder::getStatus, ChargeOrderStatus.PENDING.getCode());

        List<ChargeOrder> chargeOrderList = chargeOrderMapper.selectList(queryWrapper);

        for (ChargeOrder chargeOrder : chargeOrderList){

            LambdaQueryWrapper<EtherScanAccountTransaction> transactionQueryWrapper = new LambdaQueryWrapper<>();
            transactionQueryWrapper.eq(EtherScanAccountTransaction::getHash, chargeOrder.getHash());

            EtherScanAccountTransaction transaction = etherScanAccountTransactionMapper.selectOne(transactionQueryWrapper);

            if (Objects.isNull(transaction)){
                continue;
            }

            if (transaction.getReceiptStatus().equals("1")){
                ChargeOrderUpdateCmd chargeOrderUpdateCmd = new ChargeOrderUpdateCmd();
                chargeOrderUpdateCmd.setHash(chargeOrder.getHash());
                chargeOrderUpdateCmd.setStatus(ChargeOrderStatus.SUCCESS.getCode());
                this.update(chargeOrderUpdateCmd);
            } else if (transaction.getReceiptStatus().equals("0")) {
                ChargeOrderUpdateCmd chargeOrderUpdateCmd = new ChargeOrderUpdateCmd();
                chargeOrderUpdateCmd.setHash(chargeOrder.getHash());
                chargeOrderUpdateCmd.setStatus(ChargeOrderStatus.FAILED.getCode());
                this.update(chargeOrderUpdateCmd);
            }
        }
        return SingleResponse.buildSuccess();
    }
}
