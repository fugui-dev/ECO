package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.bean.dto.WithdrawRecordDTO;
import com.example.eco.common.AccountType;
import com.example.eco.common.MinerConfigEnum;
import com.example.eco.common.WithdrawRecordStatus;
import com.example.eco.core.service.AccountService;
import com.example.eco.core.service.WithdrawRecordService;
import com.example.eco.model.entity.MinerConfig;
import com.example.eco.model.entity.SystemConfig;
import com.example.eco.model.entity.WithdrawRecord;
import com.example.eco.model.mapper.MinerConfigMapper;
import com.example.eco.model.mapper.SystemConfigMapper;
import com.example.eco.model.mapper.WithdrawRecordMapper;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class WithdrawRecordServiceImpl implements WithdrawRecordService {

    @Resource
    private WithdrawRecordMapper withdrawRecordMapper;

    @Resource
    private AccountService accountService;

    @Resource
    private MinerConfigMapper minerConfigMapper;

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> create(WithdrawRecordCreateCmd withdrawRecordCreateCmd) {

        String order = "WR" + System.currentTimeMillis();

        AccountWithdrawNumberCmd accountWithdrawNumberCmd = new AccountWithdrawNumberCmd();
        accountWithdrawNumberCmd.setNumber(withdrawRecordCreateCmd.getNumber());
        accountWithdrawNumberCmd.setWalletAddress(withdrawRecordCreateCmd.getWalletAddress());
        accountWithdrawNumberCmd.setOrder(order);
        accountWithdrawNumberCmd.setType(withdrawRecordCreateCmd.getType());

        SingleResponse<Void> response = accountService.withdrawNumber(accountWithdrawNumberCmd);
        if (!response.isSuccess()) {
            throw new RuntimeException(response.getErrMessage());
        }

        LambdaQueryWrapper<MinerConfig> minerConfigLambdaQueryWrapper = new LambdaQueryWrapper<>();
        minerConfigLambdaQueryWrapper.eq(MinerConfig::getName, MinerConfigEnum.WITHDRAW_SERVICE.getName());

        MinerConfig minerConfig = minerConfigMapper.selectOne(minerConfigLambdaQueryWrapper);
        if (Objects.nonNull(minerConfig) && StringUtils.isNotEmpty(minerConfig.getValue())){

            AccountWithdrawServiceCmd accountWithdrawServiceCmd = new AccountWithdrawServiceCmd();
            accountWithdrawServiceCmd.setNumber(minerConfig.getValue());
            accountWithdrawServiceCmd.setWalletAddress(withdrawRecordCreateCmd.getWalletAddress());
            accountWithdrawServiceCmd.setOrder(order);
            SingleResponse<Void> serviceResponse = accountService.withdrawServiceNumber(accountWithdrawServiceCmd);
            if (!serviceResponse.isSuccess()) {

                throw new RuntimeException(serviceResponse.getErrMessage());
            }
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

        if (withdrawRecordDealWithCmd.getStatus().equals(WithdrawRecordStatus.AGREE.getCode())){

            AccountReleaseLockWithdrawNumberCmd accountReleaseLockWithdrawNumberCmd = new AccountReleaseLockWithdrawNumberCmd();
            accountReleaseLockWithdrawNumberCmd.setOrder(withdrawRecord.getOrder());
            accountReleaseLockWithdrawNumberCmd.setWalletAddress(withdrawRecord.getWalletAddress());

            SingleResponse<Void> response = accountService.releaseLockWithdrawNumber(accountReleaseLockWithdrawNumberCmd);
            if (!response.isSuccess()) {
                throw new RuntimeException(response.getErrMessage());
            }

            AccountReleaseLockWithdrawServiceCmd accountReleaseLockWithdrawServiceCmd = new AccountReleaseLockWithdrawServiceCmd();
            accountReleaseLockWithdrawServiceCmd.setOrder(withdrawRecord.getOrder());
            accountReleaseLockWithdrawServiceCmd.setWalletAddress(withdrawRecord.getWalletAddress());

            SingleResponse<Void> releaseResponse = accountService.releaseLockWithdrawServiceNumber(accountReleaseLockWithdrawServiceCmd);
            if (!releaseResponse.isSuccess()){
                throw new RuntimeException(releaseResponse.getErrMessage());
            }
        }else {

            RollbackLockWithdrawNumberCmd rollbackLockWithdrawNumberCmd = new RollbackLockWithdrawNumberCmd();
            rollbackLockWithdrawNumberCmd.setOrder(withdrawRecord.getOrder());
            rollbackLockWithdrawNumberCmd.setWalletAddress(withdrawRecord.getWalletAddress());

            SingleResponse<Void> response = accountService.rollbackLockWithdrawNumber(rollbackLockWithdrawNumberCmd);
            if (!response.isSuccess()) {
                throw new RuntimeException(response.getErrMessage());
            }

            RollbackLockWithdrawServiceCmd rollbackLockWithdrawServiceCmd = new RollbackLockWithdrawServiceCmd();
            rollbackLockWithdrawServiceCmd.setOrder(withdrawRecord.getOrder());
            rollbackLockWithdrawServiceCmd.setWalletAddress(withdrawRecord.getWalletAddress());

            SingleResponse<Void> rollbackResponse = accountService.rollbackLockWithdrawServiceNumber(rollbackLockWithdrawServiceCmd);
            if (!rollbackResponse.isSuccess()) {
                throw new RuntimeException(rollbackResponse.getErrMessage());
            }
        }

        withdrawRecord.setStatus(withdrawRecordDealWithCmd.getStatus());
        withdrawRecord.setReason(withdrawRecordDealWithCmd.getReason());
        withdrawRecord.setReviewTime(System.currentTimeMillis());
        withdrawRecordMapper.updateById(withdrawRecord);

        return SingleResponse.buildSuccess();
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> cancel(withdrawRecordCancelCmd withdrawRecordCancelCmd) {

        WithdrawRecord withdrawRecord = withdrawRecordMapper.selectById(withdrawRecordCancelCmd.getId());
        if (Objects.isNull(withdrawRecord)){
            return SingleResponse.buildFailure("提现记录不存在");
        }
        if (!withdrawRecord.getStatus().equals(WithdrawRecordStatus.PENDING_REVIEW.getCode())){
            return SingleResponse.buildFailure("提现记录已处理");
        }
        if (!withdrawRecord.getWalletAddress().equals(withdrawRecordCancelCmd.getWalletAddress())){
            return SingleResponse.buildFailure("只能取消自己的提现记录");
        }
        RollbackLockWithdrawNumberCmd rollbackLockWithdrawNumberCmd = new RollbackLockWithdrawNumberCmd();
        rollbackLockWithdrawNumberCmd.setOrder(withdrawRecord.getOrder());
        rollbackLockWithdrawNumberCmd.setWalletAddress(withdrawRecord.getWalletAddress());
        SingleResponse<Void> response = accountService.rollbackLockWithdrawNumber(rollbackLockWithdrawNumberCmd);
        if (!response.isSuccess()) {
            throw new RuntimeException(response.getErrMessage());
        }

        RollbackLockWithdrawServiceCmd rollbackLockWithdrawServiceCmd = new RollbackLockWithdrawServiceCmd();
        rollbackLockWithdrawServiceCmd.setOrder(withdrawRecord.getOrder());
        rollbackLockWithdrawServiceCmd.setWalletAddress(withdrawRecord.getWalletAddress());

        SingleResponse<Void> rollbackResponse = accountService.rollbackLockWithdrawServiceNumber(rollbackLockWithdrawServiceCmd);
        if (!rollbackResponse.isSuccess()) {
            throw new RuntimeException(rollbackResponse.getErrMessage());
        }
        withdrawRecord.setStatus(WithdrawRecordStatus.CANCELED.getCode());
        withdrawRecord.setCancelTime(System.currentTimeMillis());
        withdrawRecordMapper.updateById(withdrawRecord);
        return SingleResponse.buildSuccess();
    }

    @Override
    public MultiResponse<WithdrawRecordDTO> page(WithdrawRecordPageQry withdrawRecordPageQry) {

        LambdaQueryWrapper<WithdrawRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StringUtils.isNotEmpty(withdrawRecordPageQry.getType()), WithdrawRecord::getType, withdrawRecordPageQry.getType());
        queryWrapper.eq(StringUtils.isNotEmpty(withdrawRecordPageQry.getStatus()), WithdrawRecord::getStatus, withdrawRecordPageQry.getStatus());
        queryWrapper.eq(StringUtils.isNotEmpty(withdrawRecordPageQry.getWalletAddress()), WithdrawRecord::getWalletAddress, withdrawRecordPageQry.getWalletAddress());
        queryWrapper.eq(StringUtils.isNotEmpty(withdrawRecordPageQry.getOrder()), WithdrawRecord::getOrder, withdrawRecordPageQry.getOrder());

        Page<WithdrawRecord> withdrawRecordPage = withdrawRecordMapper.selectPage(Page.of(withdrawRecordPageQry.getPageNum(), withdrawRecordPageQry.getPageSize()), queryWrapper);

        List<WithdrawRecordDTO> withdrawRecordList = new ArrayList<>();

        for (WithdrawRecord withdrawRecord : withdrawRecordPage.getRecords()) {
            WithdrawRecordDTO withdrawRecordDTO = new WithdrawRecordDTO();
            BeanUtils.copyProperties(withdrawRecord, withdrawRecordDTO);

            withdrawRecordDTO.setTypeName(AccountType.valueOf(withdrawRecord.getType()).getName());
            withdrawRecordDTO.setStatusName(WithdrawRecordStatus.valueOf(withdrawRecord.getStatus()).getName());
            withdrawRecordList.add(withdrawRecordDTO);
        }
        return MultiResponse.of(withdrawRecordList, (int) withdrawRecordPage.getTotal());
    }
}
