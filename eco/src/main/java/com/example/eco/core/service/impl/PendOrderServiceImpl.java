package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.bean.dto.PendOrderDTO;
import com.example.eco.common.AccountType;
import com.example.eco.common.BusinessException;
import com.example.eco.common.PendOrderStatus;
import com.example.eco.core.service.AccountService;
import com.example.eco.core.service.PendOrderService;
import com.example.eco.model.entity.PendOrder;
import com.example.eco.model.mapper.PendOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
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
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PendOrderServiceImpl implements PendOrderService {

    @Resource
    private PendOrderMapper pendOrderMapper;

    @Resource
    private AccountService accountService;

    @Resource
    private RedissonClient redissonClient;

    private static final String PEND_ORDER_LOCK_KEY = "pend_order_lock:";


    @Override
    public SingleResponse<Void> createPendOrder(PendOrderCreateCmd pendOrderCreateCmd) {

        String order = "PO" + System.currentTimeMillis();

        AccountSellNumberCmd accountSellNumberCmd = new AccountSellNumberCmd();
        accountSellNumberCmd.setNumber(String.valueOf(new BigDecimal(pendOrderCreateCmd.getNumber()).add(new BigDecimal(pendOrderCreateCmd.getComplimentaryNumber()))));
        accountSellNumberCmd.setOrder(order);
        accountSellNumberCmd.setType(pendOrderCreateCmd.getType());
        accountSellNumberCmd.setWalletAddress(pendOrderCreateCmd.getWalletAddress());

        try {

            SingleResponse<Void> response = accountService.sellNumber(accountSellNumberCmd);
            if (!response.isSuccess()) {
                return response;
            }

        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("创建挂单异常");
        }

        PendOrder pendOrder = new PendOrder();
        pendOrder.setOrder(order);
        pendOrder.setWalletAddress(pendOrderCreateCmd.getWalletAddress());
        pendOrder.setType(pendOrderCreateCmd.getType());
        pendOrder.setNumber(pendOrderCreateCmd.getNumber());
        pendOrder.setComplimentaryNumber(pendOrderCreateCmd.getComplimentaryNumber());
        pendOrder.setPrice(pendOrderCreateCmd.getPrice());
        pendOrder.setTotalPrice(pendOrderCreateCmd.getTotalPrice());
        pendOrder.setStatus(PendOrderStatus.WAIT.getCode());
        pendOrder.setRecipientWalletAddress(pendOrderCreateCmd.getRecipientWalletAddress());
        pendOrder.setCreateTime(System.currentTimeMillis());
        pendOrderMapper.insert(pendOrder);
        return SingleResponse.buildSuccess();
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> deletePendOrder(PendOrderDeleteCmd pendOrderDeleteCmd) {
        RLock lock = redissonClient.getLock(PEND_ORDER_LOCK_KEY + pendOrderDeleteCmd.getOrder());
        try {
            if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                return SingleResponse.buildFailure("挂单处理中，请稍后再试");
            }

            LambdaQueryWrapper<PendOrder> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PendOrder::getOrder, pendOrderDeleteCmd.getOrder());

            PendOrder pendOrder = pendOrderMapper.selectOne(queryWrapper);
            if (pendOrder == null) {
                return SingleResponse.buildFailure("挂单不存在");
            }

            if (!pendOrder.getStatus().equals(PendOrderStatus.WAIT.getCode())) {
                return SingleResponse.buildFailure("挂单状态不允许取消");
            }

            if (!pendOrder.getWalletAddress().equals(pendOrderDeleteCmd.getWalletAddress())) {
                return SingleResponse.buildFailure("只能删除自己的锁单");
            }

            RollbackLockSellNumberCmd rollbackLockSellNumberCmd = new RollbackLockSellNumberCmd();
            rollbackLockSellNumberCmd.setOrder(pendOrderDeleteCmd.getOrder());
            rollbackLockSellNumberCmd.setWalletAddress(pendOrder.getWalletAddress());

            try {
                SingleResponse<Void> response = accountService.rollbackLockSellNumber(rollbackLockSellNumberCmd);
                if (!response.isSuccess()){
                    return response;
                }
            }catch (Exception e){
                e.printStackTrace();
                throw new RuntimeException("删除挂单异常: " + e.getMessage());
            }

            pendOrder.setStatus(PendOrderStatus.DELETE.getCode());
            pendOrderMapper.updateById(pendOrder);

            return SingleResponse.buildSuccess();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SingleResponse.buildFailure("操作被中断");
        } catch (Exception e) {
            // 重新抛出异常以确保事务回滚
            throw new RuntimeException("删除挂单失败: " + e.getMessage());
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> lockPendOrder(PendOrderLockCmd pendOrderLockCmd) {
        RLock lock = redissonClient.getLock(PEND_ORDER_LOCK_KEY + pendOrderLockCmd.getOrder());
        try {
            if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                return SingleResponse.buildFailure("锁单处理中，请稍后再试");
            }

            LambdaQueryWrapper<PendOrder> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PendOrder::getOrder, pendOrderLockCmd.getOrder());

            PendOrder pendOrder = pendOrderMapper.selectOne(queryWrapper);
            if (pendOrder == null) {
                return SingleResponse.buildFailure("挂单不存在");
            }

            if (!pendOrder.getStatus().equals(PendOrderStatus.WAIT.getCode())) {
                return SingleResponse.buildFailure("挂单状态不允许锁单");
            }

            AccountBuyNumberCmd accountBuyNumberCmd = new AccountBuyNumberCmd();
            accountBuyNumberCmd.setOrder(pendOrderLockCmd.getOrder());
            accountBuyNumberCmd.setNumber(String.valueOf(new BigDecimal(pendOrder.getNumber()).add(new BigDecimal(pendOrder.getComplimentaryNumber()))));
            accountBuyNumberCmd.setType(pendOrder.getType());
            accountBuyNumberCmd.setWalletAddress(pendOrderLockCmd.getWalletAddress());

            try {
                SingleResponse<Void> response = accountService.buyNumber(accountBuyNumberCmd);
                if (!response.isSuccess()) {
                    return  response;
                }
            }catch (Exception e){
                e.printStackTrace();
                throw new RuntimeException("锁单异常: " + e.getMessage());
            }

            pendOrder.setBuyerWalletAddress(pendOrderLockCmd.getWalletAddress());
            pendOrder.setPlaceOrderTime(System.currentTimeMillis());
            pendOrder.setStatus(PendOrderStatus.LOCK.getCode());
            pendOrder.setUpdateTime(System.currentTimeMillis());
            pendOrderMapper.updateById(pendOrder);

            return SingleResponse.buildSuccess();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SingleResponse.buildFailure("操作被中断");
        } catch (Exception e) {
            // 重新抛出异常以确保事务回滚
            throw new RuntimeException("锁单失败: " + e.getMessage());
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> cancelPendOrder(PendOrderCancelCmd pendOrderCancelCmd) {
        RLock lock = redissonClient.getLock(PEND_ORDER_LOCK_KEY + pendOrderCancelCmd.getOrder());
        try {
            if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                return SingleResponse.buildFailure("锁单处理中，请稍后再试");
            }

            LambdaQueryWrapper<PendOrder> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PendOrder::getOrder, pendOrderCancelCmd.getOrder());

            PendOrder pendOrder = pendOrderMapper.selectOne(queryWrapper);
            if (pendOrder == null) {
                return SingleResponse.buildFailure("挂单不存在");
            }

            if (!pendOrder.getBuyerWalletAddress().equals(pendOrderCancelCmd.getWalletAddress())) {
                return SingleResponse.buildFailure("只能取消自己的锁单");
            }

            if (!pendOrder.getStatus().equals(PendOrderStatus.LOCK.getCode())) {
                return SingleResponse.buildFailure("挂单状态不允许取消");
            }


            RollbackLockBuyNumberCmd rollbackLockBuyNumberCmd = new RollbackLockBuyNumberCmd();
            rollbackLockBuyNumberCmd.setOrder(pendOrderCancelCmd.getOrder());
            rollbackLockBuyNumberCmd.setWalletAddress(pendOrderCancelCmd.getWalletAddress());

            try {
                SingleResponse<Void> response = accountService.rollbackLockBuyNumber(rollbackLockBuyNumberCmd);
                if (!response.isSuccess()) {
                    return response;
                }
            }catch (Exception e){
                e.printStackTrace();
                throw new RuntimeException("取消锁单异常: " + e.getMessage());
            }

            pendOrder.setPlaceOrderTime(null);
            pendOrder.setBuyerWalletAddress(null);
            pendOrder.setStatus(PendOrderStatus.WAIT.getCode());
            pendOrder.setUpdateTime(System.currentTimeMillis());
            pendOrderMapper.updateById(pendOrder);

            return SingleResponse.buildSuccess();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SingleResponse.buildFailure("操作被中断");
        } catch (Exception e) {
            // 重新抛出异常以确保事务回滚
            throw new RuntimeException("取消锁单失败: " + e.getMessage());
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> completePendOrder(PendOrderCompleteCmd pendOrderCompleteCmd) {
        RLock lock = redissonClient.getLock(PEND_ORDER_LOCK_KEY + pendOrderCompleteCmd.getOrder());
        try {
            if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                return SingleResponse.buildFailure("确认订单处理中，请稍后再试");
            }

            LambdaQueryWrapper<PendOrder> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PendOrder::getOrder, pendOrderCompleteCmd.getOrder());

            PendOrder pendOrder = pendOrderMapper.selectOne(queryWrapper);
            if (pendOrder == null) {
                return SingleResponse.buildFailure("挂单不存在");
            }

            if (!pendOrder.getWalletAddress().equals(pendOrderCompleteCmd.getWalletAddress())) {
                return SingleResponse.buildFailure("只能确认自己的锁单");
            }

            if (!pendOrder.getStatus().equals(PendOrderStatus.APPLY.getCode())) {
                return SingleResponse.buildFailure("请等待上传支付凭证");
            }


            //释放购买金额
            AccountReleaseLockBuyNumberCmd accountReleaseLockBuyNumberCmd = new AccountReleaseLockBuyNumberCmd();
            accountReleaseLockBuyNumberCmd.setOrder(pendOrderCompleteCmd.getOrder());
            accountReleaseLockBuyNumberCmd.setWalletAddress(pendOrder.getBuyerWalletAddress());

            try {
                SingleResponse<Void> buyResponse = accountService.releaseLockBuyNumber(accountReleaseLockBuyNumberCmd);
                if (!buyResponse.isSuccess()) {
                    return buyResponse;
                }
            }catch (Exception e){
                e.printStackTrace();
                throw new RuntimeException("确认挂单异常: " + e.getMessage());
            }

            //释放销售金额
            AccountReleaseLockSellNumberCmd accountReleaseLockSellNumberCmd = new AccountReleaseLockSellNumberCmd();
            accountReleaseLockSellNumberCmd.setOrder(pendOrderCompleteCmd.getOrder());
            accountReleaseLockSellNumberCmd.setWalletAddress(pendOrder.getWalletAddress());

            try {
                SingleResponse<Void> sellResponse = accountService.releaseLockSellNumber(accountReleaseLockSellNumberCmd);
                if (!sellResponse.isSuccess()) {
                    log.info("确认挂单异常：{}",sellResponse.getErrMessage());
                    throw new BusinessException("确认挂单异常："+ sellResponse.getErrMessage());
                }
            }catch (Exception e){
                e.printStackTrace();
                throw new RuntimeException(e.getMessage());
            }

            pendOrder.setStatus(PendOrderStatus.COMPLETE.getCode());
            pendOrder.setConfirmTime(System.currentTimeMillis());
            pendOrder.setUpdateTime(System.currentTimeMillis());
            pendOrderMapper.updateById(pendOrder);

            return SingleResponse.buildSuccess();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SingleResponse.buildFailure("操作被中断");
        } catch (Exception e) {
            // 重新抛出异常以确保事务回滚
            throw new RuntimeException("确认挂单失败: " + e.getMessage());
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public SingleResponse<Void> uploadVoucher(PendOrderUploadVoucherCmd pendOrderUploadVoucherCmd) {

        PendOrder pendOrder = pendOrderMapper.selectById(pendOrderUploadVoucherCmd.getId());
        if (pendOrder == null) {
            return SingleResponse.buildFailure("挂单不存在");
        }

        if (pendOrder.getStatus().equals(PendOrderStatus.COMPLETE.getCode())) {
            return SingleResponse.buildFailure("挂单已完成，不能上传凭证");
        }

        pendOrder.setHash(pendOrderUploadVoucherCmd.getHash());
        pendOrder.setStatus(PendOrderStatus.APPLY.getCode());
        pendOrder.setImageList(pendOrderUploadVoucherCmd.getImageList());
        pendOrder.setUpdateTime(System.currentTimeMillis());
        pendOrderMapper.updateById(pendOrder);

        return SingleResponse.buildSuccess();
    }

    @Override
    public MultiResponse<PendOrderDTO> page(PendOrderPageQry pendOrderPageQry) {

        LambdaQueryWrapper<PendOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StringUtils.isNotEmpty(pendOrderPageQry.getWalletAddress()), PendOrder::getWalletAddress, pendOrderPageQry.getWalletAddress());
        queryWrapper.eq(StringUtils.isNotEmpty(pendOrderPageQry.getStatus()), PendOrder::getStatus, pendOrderPageQry.getStatus());
        queryWrapper.eq(StringUtils.isNotEmpty(pendOrderPageQry.getType()), PendOrder::getType, pendOrderPageQry.getType());
        queryWrapper.eq(StringUtils.isNotEmpty(pendOrderPageQry.getBuyerWalletAddress()), PendOrder::getBuyerWalletAddress, pendOrderPageQry.getBuyerWalletAddress());

        Page<PendOrder> pendOrderPage = pendOrderMapper.selectPage(Page.of(pendOrderPageQry.getPageNum(), pendOrderPageQry.getPageSize()), queryWrapper);

        if (CollectionUtils.isEmpty(pendOrderPage.getRecords())) {
            return MultiResponse.buildSuccess();
        }

        List<PendOrderDTO> pendOrderDTOList = new ArrayList<>();
        for (PendOrder pendOrder : pendOrderPage.getRecords()){

            PendOrderDTO pendOrderDTO = new PendOrderDTO();
            BeanUtils.copyProperties(pendOrder, pendOrderDTO);
            pendOrderDTO.setTypeName(AccountType.of(pendOrder.getType()).getName());
            pendOrderDTO.setStatusName(PendOrderStatus.of(pendOrder.getStatus()).getName());
            pendOrderDTOList.add(pendOrderDTO);
        }


        return MultiResponse.of(pendOrderDTOList, (int)pendOrderPage.getTotal());
    }
}
