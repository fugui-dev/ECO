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
import com.example.eco.core.service.EsgAccountService;
import com.example.eco.core.service.EsgChargeOrderService;
import com.example.eco.model.entity.ChargeOrder;
import com.example.eco.model.entity.EsgChargeOrder;
import com.example.eco.model.entity.TokenTransferLog;
import com.example.eco.model.mapper.ChargeOrderMapper;
import com.example.eco.model.mapper.EsgChargeOrderMapper;
import com.example.eco.model.mapper.TokenTransferLogMapper;
import com.example.eco.util.TransactionVerificationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EsgChargeOrderServiceImpl implements EsgChargeOrderService {

    @Resource
    private EsgChargeOrderMapper chargeOrderMapper;

    @Resource
    private EsgAccountService accountService;

    @Resource
    private TransactionVerificationUtil transactionVerificationUtil;

    @Resource
    private TokenTransferLogMapper tokenTransferLogMapper;


    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> create(ChargeOrderCreateCmd chargeOrderCreateCmd) {

        String order = "ECO" + System.currentTimeMillis();

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
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("创建充值订单异常");
        }

        EsgChargeOrder chargeOrder = new EsgChargeOrder();
        chargeOrder.setOrder(order);
        chargeOrder.setWalletAddress(chargeOrderCreateCmd.getWalletAddress());
        chargeOrder.setNumber(chargeOrderCreateCmd.getNumber());
        chargeOrder.setStatus(ChargeOrderStatus.PENDING.getCode());
        chargeOrder.setHash(chargeOrderCreateCmd.getHash());
        chargeOrder.setCreateTime(System.currentTimeMillis());

        chargeOrderMapper.insert(chargeOrder);

        return SingleResponse.buildSuccess();
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> update(ChargeOrderUpdateCmd chargeOrderUpdateCmd) {

        LambdaQueryWrapper<EsgChargeOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EsgChargeOrder::getHash, chargeOrderUpdateCmd.getHash());

        EsgChargeOrder chargeOrder = chargeOrderMapper.selectOne(queryWrapper);
        if (Objects.isNull(chargeOrder)) {
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
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("更新充值订单失败");
            }
        } else {

            chargeOrder.setStatus(ChargeOrderStatus.FAILED.getCode());
            chargeOrder.setFinishTime(System.currentTimeMillis());

            RollbackLockChargeNumberCmd rollbackLockChargeNumberCmd = new RollbackLockChargeNumberCmd();
            rollbackLockChargeNumberCmd.setWalletAddress(chargeOrder.getWalletAddress());
            rollbackLockChargeNumberCmd.setOrder(chargeOrder.getOrder());
            rollbackLockChargeNumberCmd.setHash(chargeOrder.getHash());

            try {
                SingleResponse<Void> response = accountService.rollbackLockChargeNumber(rollbackLockChargeNumberCmd);

                if (!response.isSuccess()) {
                    log.info("更新充值订单失败{}", response.getErrMessage());
                    throw new RuntimeException("更新充值订单失败");
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("更新充值订单失败");
            }
        }

        chargeOrderMapper.updateById(chargeOrder);
        return SingleResponse.buildSuccess();
    }

    @Override
    public MultiResponse<ChargeOrderDTO> page(ChargeOrderPageQry chargeOrderPageQry) {

        LambdaQueryWrapper<EsgChargeOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StringUtils.isNotEmpty(chargeOrderPageQry.getWalletAddress()), EsgChargeOrder::getWalletAddress, chargeOrderPageQry.getWalletAddress());
        queryWrapper.eq(StringUtils.isNotEmpty(chargeOrderPageQry.getStatus()), EsgChargeOrder::getStatus, chargeOrderPageQry.getStatus());
        queryWrapper.eq(StringUtils.isNotEmpty(chargeOrderPageQry.getOrder()), EsgChargeOrder::getOrder, chargeOrderPageQry.getOrder());

        Page<EsgChargeOrder> chargeOrderPage = chargeOrderMapper.selectPage(Page.of(chargeOrderPageQry.getPageNum(), chargeOrderPageQry.getPageSize()), queryWrapper);

        List<ChargeOrderDTO> chargeOrderList = new ArrayList<>();

        for (EsgChargeOrder chargeOrder : chargeOrderPage.getRecords()) {
            ChargeOrderDTO chargeOrderDTO = new ChargeOrderDTO();
            BeanUtils.copyProperties(chargeOrder, chargeOrderDTO);

            chargeOrderDTO.setStatusName(ChargeOrderStatus.of(chargeOrder.getStatus()).getName());
            chargeOrderList.add(chargeOrderDTO);
        }
        return MultiResponse.of(chargeOrderList, (int) chargeOrderPage.getTotal());
    }

    @Override
    public SingleResponse<Void> checkChargeOrder() {


        LambdaQueryWrapper<EsgChargeOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EsgChargeOrder::getStatus, ChargeOrderStatus.PENDING.getCode());

        List<EsgChargeOrder> chargeOrderList = chargeOrderMapper.selectList(queryWrapper);

        for (EsgChargeOrder chargeOrder : chargeOrderList) {

            if (StringUtils.isEmpty(chargeOrder.getHash())) {
                continue;
            }

            Boolean transaction = transactionVerificationUtil.verifyTransaction(chargeOrder.getHash(),
                    chargeOrder.getNumber(),
                    "ESG-NFT",
                    chargeOrder.getWalletAddress());


            if (Objects.isNull(transaction)) {
                continue;
            }

            if (transaction) {

                ChargeOrderUpdateCmd chargeOrderUpdateCmd = new ChargeOrderUpdateCmd();
                chargeOrderUpdateCmd.setHash(chargeOrder.getHash());
                chargeOrderUpdateCmd.setStatus(ChargeOrderStatus.SUCCESS.getCode());
                this.update(chargeOrderUpdateCmd);
            } else {
                ChargeOrderUpdateCmd chargeOrderUpdateCmd = new ChargeOrderUpdateCmd();
                chargeOrderUpdateCmd.setHash(chargeOrder.getHash());
                chargeOrderUpdateCmd.setStatus(ChargeOrderStatus.FAILED.getCode());
                this.update(chargeOrderUpdateCmd);
            }

        }
        return SingleResponse.buildSuccess();
    }

    @Transactional(rollbackFor = Exception.class)
    public SingleResponse<Void> dealwithFailChargeOrder() {
        // 1. 一次性查询所有需要处理的订单
        LambdaQueryWrapper<EsgChargeOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EsgChargeOrder::getStatus, ChargeOrderStatus.FAILED.getCode())
                .isNotNull(EsgChargeOrder::getHash); // 提前过滤空hash

        List<EsgChargeOrder> chargeOrderList = chargeOrderMapper.selectList(queryWrapper);

        if (CollectionUtils.isEmpty(chargeOrderList)) {
            return SingleResponse.buildSuccess();
        }

        // 2. 批量查询相关的TokenTransferLog
        List<String> hashes = chargeOrderList.stream()
                .map(EsgChargeOrder::getHash)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(hashes)) {
            return SingleResponse.buildSuccess();
        }

        LambdaQueryWrapper<TokenTransferLog> logQueryWrapper = new LambdaQueryWrapper<>();
        logQueryWrapper.in(TokenTransferLog::getHash, hashes)
                .eq(TokenTransferLog::getStatus, "SUCCESS"); // 数据库层面过滤

        List<TokenTransferLog> successLogs = tokenTransferLogMapper.selectList(logQueryWrapper);

        // 3. 构建hash到log的映射
        Map<String, TokenTransferLog> successLogMap = successLogs.stream()
                .collect(Collectors.toMap(TokenTransferLog::getHash, Function.identity()));

        List<Integer> deleteOrderIds = new ArrayList<>();
        List<TokenTransferLog> logsToUpdate = new ArrayList<>();

        // 4. 处理逻辑
        for (EsgChargeOrder chargeOrder : chargeOrderList) {
            TokenTransferLog log = successLogMap.get(chargeOrder.getHash());
            if (log != null) {
                log.setChecked(Boolean.FALSE);
                logsToUpdate.add(log);
                deleteOrderIds.add(chargeOrder.getId());
            }
        }

        // 5. 批量更新和删除
        if (!logsToUpdate.isEmpty()) {
            // 如果mybatis-plus支持批量更新，使用批量方法
            for (TokenTransferLog log : logsToUpdate) {
                tokenTransferLogMapper.updateById(log);
            }
            // 或者使用批量更新：tokenTransferLogService.updateBatchById(logsToUpdate);
        }

        if (!deleteOrderIds.isEmpty()) {
            LambdaQueryWrapper<EsgChargeOrder> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.in(EsgChargeOrder::getId, deleteOrderIds);
            chargeOrderMapper.delete(deleteWrapper);
        }

        return SingleResponse.buildSuccess();
    }
}
