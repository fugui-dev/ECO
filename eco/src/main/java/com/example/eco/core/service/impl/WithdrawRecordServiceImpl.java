package com.example.eco.core.service.impl;

import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.AccountWithdrawNumberCmd;
import com.example.eco.bean.cmd.WithdrawRecordCreateCmd;
import com.example.eco.common.WithdrawRecordStatus;
import com.example.eco.core.service.AccountService;
import com.example.eco.core.service.WithdrawRecordService;
import com.example.eco.model.entity.WithdrawRecord;
import com.example.eco.model.mapper.WithdrawRecordMapper;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

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
}
