package com.example.eco.core.service.impl;

import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.common.WithdrawRecordStatus;
import com.example.eco.core.service.AccountService;
import com.example.eco.core.service.WithdrawRecordService;
import com.example.eco.model.entity.WithdrawRecord;
import com.example.eco.model.mapper.WithdrawRecordMapper;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@Service
public class WithdrawRecordServiceImpl implements WithdrawRecordService {

    @Resource
    private WithdrawRecordMapper withdrawRecordMapper;

    @Resource
    private AccountService accountService;

    @Resource
    private RedissonClient redissonClient;


    private static final String PEND_ORDER_LOCK_KEY = "withdraw_record_lock:";

    @Override
    public SingleResponse<Void> create(WithdrawRecordCreateCmd withdrawRecordCreateCmd) {

        String order = "WR" + System.currentTimeMillis();

        AccountWithdrawNumberCmd accountWithdrawNumberCmd = new AccountWithdrawNumberCmd();
        accountWithdrawNumberCmd.setNumber(withdrawRecordCreateCmd.getNumber());
        accountWithdrawNumberCmd.setWalletAddress(withdrawRecordCreateCmd.getWalletAddress());
        accountWithdrawNumberCmd.setOrder(order);
        accountWithdrawNumberCmd.setType(withdrawRecordCreateCmd.getType());

        SingleResponse<Void> response = accountService.withdrawNumber(accountWithdrawNumberCmd);
        if (!response.isSuccess()) {
            return response;
        }

        WithdrawRecord withdrawRecord = new WithdrawRecord();
        withdrawRecord.setWalletAddress(withdrawRecordCreateCmd.getWalletAddress());
        withdrawRecord.setOrder(order);
        withdrawRecord.setWithdrawTime(System.currentTimeMillis());
        withdrawRecord.setType(withdrawRecordCreateCmd.getType());
        withdrawRecord.setStatus(WithdrawRecordStatus.PENDING_REVIEW.getCode());
        withdrawRecord.setWithdrawNumber(withdrawRecordCreateCmd.getNumber());
        withdrawRecord.setRemark(withdrawRecordCreateCmd.getRemark());

        withdrawRecordMapper.insert(withdrawRecord);

        return SingleResponse.buildSuccess();
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> dealWith(WithdrawRecordDealWithCmd withdrawRecordDealWithCmd) {

        WithdrawRecord withdrawRecord = withdrawRecordMapper.selectById(withdrawRecordDealWithCmd.getId());

        if (Objects.isNull(withdrawRecord)){
            return SingleResponse.buildFailure("提现记录不存在");
        }

        if (!withdrawRecord.getStatus().equals(WithdrawRecordStatus.PENDING_REVIEW.getCode())){
            return SingleResponse.buildFailure("提现记录已处理");
        }

        if (withdrawRecord.getStatus().equals(WithdrawRecordStatus.AGREE.getCode())){

            AccountReleaseLockWithdrawNumberCmd accountReleaseLockWithdrawNumberCmd = new AccountReleaseLockWithdrawNumberCmd();
            accountReleaseLockWithdrawNumberCmd.setOrder(withdrawRecord.getOrder());
            accountReleaseLockWithdrawNumberCmd.setWalletAddress(withdrawRecord.getWalletAddress());

            SingleResponse<Void> response = accountService.releaseLockWithdrawNumber(accountReleaseLockWithdrawNumberCmd);
            if (!response.isSuccess()) {
                return response;
            }
        }else {

            RollbackLockWithdrawNumberCmd rollbackLockWithdrawNumberCmd = new RollbackLockWithdrawNumberCmd();
            rollbackLockWithdrawNumberCmd.setOrder(withdrawRecord.getOrder());
            rollbackLockWithdrawNumberCmd.setWalletAddress(withdrawRecord.getWalletAddress());

            SingleResponse<Void> response = accountService.rollbackLockWithdrawNumber(rollbackLockWithdrawNumberCmd);
            if (!response.isSuccess()) {
                return response;
            }
        }

        return SingleResponse.buildSuccess();
    }
}
