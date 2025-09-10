package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.common.PendOrderStatus;
import com.example.eco.core.service.AccountService;
import com.example.eco.core.service.PendOrderService;
import com.example.eco.model.entity.PendOrder;
import com.example.eco.model.mapper.PendOrderMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

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
        accountSellNumberCmd.setNumber(pendOrderCreateCmd.getNumber());
        accountSellNumberCmd.setOrder(order);
        accountSellNumberCmd.setType(pendOrderCreateCmd.getType());
        accountSellNumberCmd.setWalletAddress(pendOrderCreateCmd.getWalletAddress());

        SingleResponse<Void> response = accountService.sellNumber(accountSellNumberCmd);
        if (!response.isSuccess()) {
            return response;
        }

        PendOrder pendOrder = new PendOrder();
        pendOrder.setOrder(order);
        pendOrder.setWalletAddress(pendOrderCreateCmd.getWalletAddress());
        pendOrder.setType(pendOrderCreateCmd.getType());
        pendOrder.setNumber(pendOrderCreateCmd.getNumber());
        pendOrder.setPrice(pendOrderCreateCmd.getPrice());
        pendOrder.setTotalPrice(pendOrderCreateCmd.getTotalPrice());
        pendOrder.setStatus(PendOrderStatus.WAIT.getCode());

        pendOrderMapper.insert(pendOrder);
        return SingleResponse.buildSuccess();
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
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

            pendOrder.setStatus(PendOrderStatus.DELETE.getCode());
            pendOrderMapper.updateById(pendOrder);

            RollbackLockSellNumberCmd rollbackLockSellNumberCmd = new RollbackLockSellNumberCmd();
            rollbackLockSellNumberCmd.setOrder(pendOrderDeleteCmd.getOrder());
            rollbackLockSellNumberCmd.setWalletAddress(pendOrder.getWalletAddress());
            accountService.rollbackLockSellNumber(rollbackLockSellNumberCmd);

            return SingleResponse.buildSuccess();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SingleResponse.buildFailure("操作被中断");
        } catch (Exception e) {
            return SingleResponse.buildFailure("删除挂单失败: " + e.getMessage());
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
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

            pendOrder.setStatus(PendOrderStatus.LOCK.getCode());
            pendOrderMapper.updateById(pendOrder);

            AccountBuyNumberCmd accountBuyNumberCmd = new AccountBuyNumberCmd();
            accountBuyNumberCmd.setOrder(pendOrderLockCmd.getOrder());
            accountBuyNumberCmd.setNumber(pendOrder.getNumber());
            accountBuyNumberCmd.setType(pendOrder.getType());
            accountBuyNumberCmd.setWalletAddress(pendOrderLockCmd.getWalletAddress());
            accountService.buyNumber(accountBuyNumberCmd);

            return SingleResponse.buildSuccess();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SingleResponse.buildFailure("操作被中断");
        } catch (Exception e) {
            return SingleResponse.buildFailure("锁单失败: " + e.getMessage());
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
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

            if (!pendOrder.getStatus().equals(PendOrderStatus.WAIT.getCode())) {
                return SingleResponse.buildFailure("挂单状态不允许锁单");
            }

            pendOrder.setStatus(PendOrderStatus.CANCEL.getCode());
            pendOrder.setCancelTime(System.currentTimeMillis());
            pendOrder.setUpdateTime(System.currentTimeMillis());
            pendOrderMapper.updateById(pendOrder);


            return SingleResponse.buildSuccess();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SingleResponse.buildFailure("操作被中断");
        } catch (Exception e) {
            return SingleResponse.buildFailure("锁单失败: " + e.getMessage());
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public SingleResponse<Void> completePendOrder(PendOrderCompleteCmd pendOrderCompleteCmd) {
        return null;
    }
}
