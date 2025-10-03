package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.bean.dto.WithdrawRecordDTO;
import com.example.eco.bean.dto.WithdrawRecordBatchResultDTO;
import com.example.eco.common.AccountType;
import com.example.eco.common.BusinessException;
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
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
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

        try {

            SingleResponse<Void> response = accountService.withdrawNumber(accountWithdrawNumberCmd);
            if (!response.isSuccess()) {
                return response;
            }
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("创建提现异常");
        }

        LambdaQueryWrapper<MinerConfig> minerConfigLambdaQueryWrapper = new LambdaQueryWrapper<>();
        minerConfigLambdaQueryWrapper.eq(MinerConfig::getName, MinerConfigEnum.WITHDRAW_SERVICE.getCode());

        MinerConfig minerConfig = minerConfigMapper.selectOne(minerConfigLambdaQueryWrapper);
        if (Objects.nonNull(minerConfig) && StringUtils.isNotEmpty(minerConfig.getValue())){

            AccountWithdrawServiceCmd accountWithdrawServiceCmd = new AccountWithdrawServiceCmd();
            accountWithdrawServiceCmd.setNumber(minerConfig.getValue());
            accountWithdrawServiceCmd.setWalletAddress(withdrawRecordCreateCmd.getWalletAddress());
            accountWithdrawServiceCmd.setOrder(order);

            try {
                SingleResponse<Void> serviceResponse = accountService.withdrawServiceNumber(accountWithdrawServiceCmd);
                if (!serviceResponse.isSuccess()) {
                    log.info("创建提现异常:{}",serviceResponse.getErrMessage());
                    throw new BusinessException("创建提现异常："+serviceResponse.getErrMessage());
                }
            }catch (Exception e){
                e.printStackTrace();
                throw new BusinessException(e.getMessage());
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
        withdrawRecord.setCreateTime(System.currentTimeMillis());
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

        if (withdrawRecord.getStatus().equals(WithdrawRecordStatus.AGREE.getCode()) ||
                withdrawRecord.getStatus().equals(WithdrawRecordStatus.REJECT.getCode())){
            return SingleResponse.buildFailure("提现记录已处理");
        }

        if (withdrawRecordDealWithCmd.getStatus().equals(WithdrawRecordStatus.IN_PROGRESS.getCode())){
            withdrawRecord.setStatus(WithdrawRecordStatus.IN_PROGRESS.getCode());
            withdrawRecordMapper.updateById(withdrawRecord);
            return SingleResponse.buildSuccess();
        }

        if (withdrawRecordDealWithCmd.getStatus().equals(WithdrawRecordStatus.AGREE.getCode())){

            AccountReleaseLockWithdrawNumberCmd accountReleaseLockWithdrawNumberCmd = new AccountReleaseLockWithdrawNumberCmd();
            accountReleaseLockWithdrawNumberCmd.setOrder(withdrawRecord.getOrder());
            accountReleaseLockWithdrawNumberCmd.setWalletAddress(withdrawRecord.getWalletAddress());

            try {
                SingleResponse<Void> response = accountService.releaseLockWithdrawNumber(accountReleaseLockWithdrawNumberCmd);
                if (!response.isSuccess()) {
                    return response;
                }
            }catch (Exception e){
                e.printStackTrace();
                throw new RuntimeException("处理提现异常");
            }

            AccountReleaseLockWithdrawServiceCmd accountReleaseLockWithdrawServiceCmd = new AccountReleaseLockWithdrawServiceCmd();
            accountReleaseLockWithdrawServiceCmd.setOrder(withdrawRecord.getOrder());
            accountReleaseLockWithdrawServiceCmd.setWalletAddress(withdrawRecord.getWalletAddress());

            try {
                SingleResponse<Void> releaseResponse = accountService.releaseLockWithdrawServiceNumber(accountReleaseLockWithdrawServiceCmd);
                if (!releaseResponse.isSuccess()){

                    log.info("处理提现异常:{}",releaseResponse.getErrMessage());
                    throw new BusinessException("处理提现异常:" + releaseResponse.getErrMessage());
                }
            }catch (Exception e){
                e.printStackTrace();
                throw new BusinessException(e.getMessage());
            }
        }else {

            RollbackLockWithdrawNumberCmd rollbackLockWithdrawNumberCmd = new RollbackLockWithdrawNumberCmd();
            rollbackLockWithdrawNumberCmd.setOrder(withdrawRecord.getOrder());
            rollbackLockWithdrawNumberCmd.setWalletAddress(withdrawRecord.getWalletAddress());

            try {
                SingleResponse<Void> response = accountService.rollbackLockWithdrawNumber(rollbackLockWithdrawNumberCmd);
                if (!response.isSuccess()) {
                    return response;
                }
            }catch (Exception e){
                e.printStackTrace();
                throw new RuntimeException("处理提现异常");
            }


            RollbackLockWithdrawServiceCmd rollbackLockWithdrawServiceCmd = new RollbackLockWithdrawServiceCmd();
            rollbackLockWithdrawServiceCmd.setOrder(withdrawRecord.getOrder());
            rollbackLockWithdrawServiceCmd.setWalletAddress(withdrawRecord.getWalletAddress());

            try {
                SingleResponse<Void> rollbackResponse = accountService.rollbackLockWithdrawServiceNumber(rollbackLockWithdrawServiceCmd);
                if (!rollbackResponse.isSuccess()) {
                    log.info("处理提现异常:{}",rollbackResponse.getErrMessage());
                    throw new BusinessException("处理提现异常:" + rollbackResponse.getErrMessage());
                }
            }catch (Exception e){
                e.printStackTrace();
                throw new BusinessException(e.getMessage());
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

        try {

            SingleResponse<Void> response = accountService.rollbackLockWithdrawNumber(rollbackLockWithdrawNumberCmd);
            if (!response.isSuccess()) {
                return response;
            }
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("取消提现异常");
        }


        RollbackLockWithdrawServiceCmd rollbackLockWithdrawServiceCmd = new RollbackLockWithdrawServiceCmd();
        rollbackLockWithdrawServiceCmd.setOrder(withdrawRecord.getOrder());
        rollbackLockWithdrawServiceCmd.setWalletAddress(withdrawRecord.getWalletAddress());

        try {
            SingleResponse<Void> rollbackResponse = accountService.rollbackLockWithdrawServiceNumber(rollbackLockWithdrawServiceCmd);
            if (!rollbackResponse.isSuccess()) {
                log.info("取消提现异常:{}",rollbackResponse.getErrMessage());
                throw new BusinessException("取消提现异常:" + rollbackResponse.getErrMessage());
            }
        }catch (Exception e){
            e.printStackTrace();
            throw new BusinessException(e.getMessage());
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
    
    @Override
    public SingleResponse<WithdrawRecordBatchResultDTO> batchDealWith(WithdrawRecordBatchDealWithCmd batchDealWithCmd) {
        log.info("开始批量处理提现记录，数量: {}", batchDealWithCmd.getIds().size());
        
        WithdrawRecordBatchResultDTO result = new WithdrawRecordBatchResultDTO();
        result.setTotalCount(batchDealWithCmd.getIds().size());
        result.setSuccessCount(0);
        result.setFailureCount(0);
        result.setSuccessIds(new ArrayList<>());
        result.setFailureDetails(new ArrayList<>());
        
        // 验证参数
        if (batchDealWithCmd.getIds() == null || batchDealWithCmd.getIds().isEmpty()) {
            return SingleResponse.buildFailure("提现记录ID列表不能为空");
        }
        
        if (StringUtils.isEmpty(batchDealWithCmd.getStatus())) {
            return SingleResponse.buildFailure("提现状态不能为空");
        }
        
        // 如果是拒绝操作，必须提供拒绝原因
        if (WithdrawRecordStatus.REJECT.getCode().equals(batchDealWithCmd.getStatus()) 
            && StringUtils.isEmpty(batchDealWithCmd.getReason())) {
            return SingleResponse.buildFailure("拒绝提现必须提供拒绝原因");
        }
        
        // 先检查所有记录是否都可以处理（避免部分处理的情况）
        List<WithdrawRecord> recordsToProcess = new ArrayList<>();
        for (Integer id : batchDealWithCmd.getIds()) {
            WithdrawRecord record = withdrawRecordMapper.selectById(id);
            if (record == null) {
                WithdrawRecordBatchResultDTO.FailureDetail failure = new WithdrawRecordBatchResultDTO.FailureDetail();
                failure.setId(id);
                failure.setReason("提现记录不存在");
                result.getFailureDetails().add(failure);
                result.setFailureCount(result.getFailureCount() + 1);
                continue;
            }
            
            if (WithdrawRecordStatus.AGREE.getCode().equals(record.getStatus()) ||
                    WithdrawRecordStatus.REJECT.getCode().equals(record.getStatus())) {
                WithdrawRecordBatchResultDTO.FailureDetail failure = new WithdrawRecordBatchResultDTO.FailureDetail();
                failure.setId(id);
                failure.setReason("提现记录已处理，当前状态: " + record.getStatus());
                result.getFailureDetails().add(failure);
                result.setFailureCount(result.getFailureCount() + 1);
                continue;
            }

            if (batchDealWithCmd.getStatus().equals(WithdrawRecordStatus.IN_PROGRESS.getCode())){

                record.setStatus(WithdrawRecordStatus.IN_PROGRESS.getCode());
                record.setUpdateTime(System.currentTimeMillis());

                withdrawRecordMapper.updateById(record);
                continue;
            }

            if (batchDealWithCmd.getStatus().equals(WithdrawRecordStatus.PENDING_REVIEW.getCode())
                    && record.getStatus().equals(WithdrawRecordStatus.IN_PROGRESS.getCode())){

                record.setStatus(WithdrawRecordStatus.PENDING_REVIEW.getCode());
                record.setUpdateTime(System.currentTimeMillis());

                withdrawRecordMapper.updateById(record);
                continue;
            }
            
            recordsToProcess.add(record);
        }
        
        // 如果所有记录都无法处理，直接返回
        if (recordsToProcess.isEmpty()) {
            log.warn("没有可处理的提现记录");
            return SingleResponse.of(result);
        }
        
        // 使用乐观锁机制批量处理
        int batchSize = 10; // 每批处理10条记录
        for (int i = 0; i < recordsToProcess.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, recordsToProcess.size());
            List<WithdrawRecord> batch = recordsToProcess.subList(i, endIndex);
            
            processBatch(batch, batchDealWithCmd, result);
        }
        
        log.info("批量处理提现记录完成，成功: {}, 失败: {}", result.getSuccessCount(), result.getFailureCount());
        return SingleResponse.of(result);
    }
    
    /**
     * 处理一批提现记录（每个记录独立事务）
     */
    private void processBatch(List<WithdrawRecord> batch, WithdrawRecordBatchDealWithCmd batchDealWithCmd, 
                             WithdrawRecordBatchResultDTO result) {
        for (WithdrawRecord record : batch) {
            try {
                // 每个记录使用独立事务处理
                processSingleRecord(record, batchDealWithCmd, result);
            } catch (Exception e) {
                WithdrawRecordBatchResultDTO.FailureDetail failure = new WithdrawRecordBatchResultDTO.FailureDetail();
                failure.setId(record.getId());
                failure.setReason("处理异常: " + e.getMessage());
                result.getFailureDetails().add(failure);
                result.setFailureCount(result.getFailureCount() + 1);
                log.error("提现记录 {} 处理异常", record.getId(), e);
            }
        }
    }
    
    /**
     * 处理单个提现记录（独立事务）
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public void processSingleRecord(WithdrawRecord record, WithdrawRecordBatchDealWithCmd batchDealWithCmd, 
                                   WithdrawRecordBatchResultDTO result) {
        // 使用乐观锁重新查询记录状态
        WithdrawRecord currentRecord = withdrawRecordMapper.selectById(record.getId());
        if (currentRecord == null) {
            WithdrawRecordBatchResultDTO.FailureDetail failure = new WithdrawRecordBatchResultDTO.FailureDetail();
            failure.setId(record.getId());
            failure.setReason("提现记录不存在");
            result.getFailureDetails().add(failure);
            result.setFailureCount(result.getFailureCount() + 1);
            return;
        }
        
//        if (!WithdrawRecordStatus.PENDING_REVIEW.getCode().equals(currentRecord.getStatus()) ||
//                !WithdrawRecordStatus.IN_PROGRESS.getCode().equals(currentRecord.getStatus())) {
//            WithdrawRecordBatchResultDTO.FailureDetail failure = new WithdrawRecordBatchResultDTO.FailureDetail();
//            failure.setId(record.getId());
//            failure.setReason("提现记录已被其他操作处理，当前状态: " + currentRecord.getStatus());
//            result.getFailureDetails().add(failure);
//            result.setFailureCount(result.getFailureCount() + 1);
//            return;
//        }
        
        // 创建单个处理命令
        WithdrawRecordDealWithCmd singleCmd = new WithdrawRecordDealWithCmd();
        singleCmd.setId(record.getId());
        singleCmd.setStatus(batchDealWithCmd.getStatus());
        singleCmd.setReason(batchDealWithCmd.getReason());
        
        // 调用单个处理方法
        SingleResponse<Void> singleResult = this.dealWith(singleCmd);
        
        if (singleResult.isSuccess()) {
            result.getSuccessIds().add(record.getId());
            result.setSuccessCount(result.getSuccessCount() + 1);
            log.info("提现记录 {} 处理成功", record.getId());
        } else {
            WithdrawRecordBatchResultDTO.FailureDetail failure = new WithdrawRecordBatchResultDTO.FailureDetail();
            failure.setId(record.getId());
            failure.setReason(singleResult.getErrMessage());
            result.getFailureDetails().add(failure);
            result.setFailureCount(result.getFailureCount() + 1);
            log.warn("提现记录 {} 处理失败: {}", record.getId(), singleResult.getErrMessage());
        }
    }
}
