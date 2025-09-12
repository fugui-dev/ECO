package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.AccountDeductCmd;
import com.example.eco.bean.cmd.PurchaseMinerProjectPageQry;
import com.example.eco.bean.cmd.PurchaseMinerProjectRewardCmd;
import com.example.eco.bean.cmd.PurchaseMinerProjectsCreateCmd;
import com.example.eco.bean.dto.PurchaseMinerProjectDTO;
import com.example.eco.common.*;
import com.example.eco.core.service.AccountService;
import com.example.eco.core.service.PurchaseMinerProjectService;
import com.example.eco.model.entity.*;
import com.example.eco.model.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
@Slf4j
@Service
public class PurchaseMinerProjectServiceImpl implements PurchaseMinerProjectService {

    @Resource
    private PurchaseMinerProjectMapper purchaseMinerProjectMapper;
    @Resource
    private MinerProjectMapper minerProjectMapper;
    @Resource
    private AccountService accountService;
    @Resource
    private SystemConfigMapper systemConfigMapper;
    @Resource
    private MinerConfigMapper minerConfigMapper;


    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> create(PurchaseMinerProjectsCreateCmd purchaseMinerProjectsCreateCmd) {

        MinerProject minerProject = minerProjectMapper.selectById(purchaseMinerProjectsCreateCmd.getMinerProjectId());
        if (minerProject == null) {
            return SingleResponse.buildFailure("矿机不存在");
        }

        String order = "PMP" + System.currentTimeMillis();

        PurchaseMinerProject purchaseMinerProject = new PurchaseMinerProject();
        purchaseMinerProject.setMinerProjectId(purchaseMinerProjectsCreateCmd.getMinerProjectId());
        purchaseMinerProject.setPrice(minerProject.getPrice());
        purchaseMinerProject.setComputingPower(minerProject.getComputingPower());
        purchaseMinerProject.setType(purchaseMinerProjectsCreateCmd.getType());
        purchaseMinerProject.setWalletAddress(purchaseMinerProjectsCreateCmd.getWalletAddress());
        purchaseMinerProject.setStatus(PurchaseMinerProjectStatus.DEALING.getCode());
        purchaseMinerProject.setOrder(order);
        purchaseMinerProject.setCreateTime(System.currentTimeMillis());

        purchaseMinerProjectMapper.insert(purchaseMinerProject);

        if (purchaseMinerProjectsCreateCmd.getType().equals(PurchaseMinerType.ESG.getCode())) {

            AccountDeductCmd accountDeductCmd = new AccountDeductCmd();
            accountDeductCmd.setAccountType(AccountType.ESG.getCode());
            accountDeductCmd.setNumber(minerProject.getPrice());
            accountDeductCmd.setWalletAddress(purchaseMinerProjectsCreateCmd.getWalletAddress());
            accountDeductCmd.setOrder(order);
            SingleResponse<Void> response = accountService.purchaseMinerProjectNumber(accountDeductCmd);
            if (!response.isSuccess()) {

                purchaseMinerProject.setStatus(PurchaseMinerProjectStatus.FAIL.getCode());
                purchaseMinerProject.setReason(response.getErrMessage());
                purchaseMinerProject.setFinishTime(System.currentTimeMillis());
                purchaseMinerProjectMapper.updateById(purchaseMinerProject);
                return response;
            } else {
                // 购买成功，记录购买信息
                purchaseMinerProject.setStatus(PurchaseMinerProjectStatus.SUCCESS.getCode());
                purchaseMinerProject.setFinishTime(System.currentTimeMillis());
                purchaseMinerProjectMapper.updateById(purchaseMinerProject);
                return SingleResponse.buildSuccess();
            }

        }



        if (purchaseMinerProjectsCreateCmd.getType().equals(PurchaseMinerType.ECO.getCode())) {

            AccountDeductCmd accountDeductCmd = new AccountDeductCmd();
            accountDeductCmd.setAccountType(AccountType.ECO.getCode());
            accountDeductCmd.setNumber(minerProject.getPrice());
            accountDeductCmd.setWalletAddress(purchaseMinerProjectsCreateCmd.getWalletAddress());
            accountDeductCmd.setOrder(order);
            SingleResponse<Void> response = accountService.purchaseMinerProjectNumber(accountDeductCmd);
            if (!response.isSuccess()) {

                purchaseMinerProject.setStatus(PurchaseMinerProjectStatus.FAIL.getCode());
                purchaseMinerProject.setReason(response.getErrMessage());
                purchaseMinerProjectMapper.updateById(purchaseMinerProject);
                return response;
            } else {
                // 购买成功，记录购买信息
                purchaseMinerProject.setStatus(PurchaseMinerProjectStatus.SUCCESS.getCode());
                purchaseMinerProjectMapper.updateById(purchaseMinerProject);
                return SingleResponse.buildSuccess();
            }

        }


        if (purchaseMinerProjectsCreateCmd.getType().equals(PurchaseMinerType.ECO_ESG.getCode())) {

            AccountDeductCmd ecoAccountDeductCmd = new AccountDeductCmd();
            ecoAccountDeductCmd.setAccountType(AccountType.ECO.getCode());
            ecoAccountDeductCmd.setNumber(new BigDecimal(minerProject.getPrice()).divide(new BigDecimal(2), 4, BigDecimal.ROUND_HALF_UP).toString());
            ecoAccountDeductCmd.setWalletAddress(purchaseMinerProjectsCreateCmd.getWalletAddress());
            ecoAccountDeductCmd.setOrder(order);
            SingleResponse<Void> ecoResponse = accountService.purchaseMinerProjectNumber(ecoAccountDeductCmd);
            if (!ecoResponse.isSuccess()) {

                purchaseMinerProject.setStatus(PurchaseMinerProjectStatus.FAIL.getCode());
                purchaseMinerProject.setReason(ecoResponse.getErrMessage());
                purchaseMinerProjectMapper.updateById(purchaseMinerProject);
                return ecoResponse;
            }

            AccountDeductCmd esgAccountDeductCmd = new AccountDeductCmd();
            esgAccountDeductCmd.setAccountType(AccountType.ECO.getCode());
            esgAccountDeductCmd.setNumber(new BigDecimal(minerProject.getPrice()).divide(new BigDecimal(2), 4, BigDecimal.ROUND_HALF_UP).toString());
            esgAccountDeductCmd.setWalletAddress(purchaseMinerProjectsCreateCmd.getWalletAddress());
            esgAccountDeductCmd.setOrder(order);
            SingleResponse<Void> esgResponse = accountService.purchaseMinerProjectNumber(esgAccountDeductCmd);
            if (!esgResponse.isSuccess()) {

                purchaseMinerProject.setStatus(PurchaseMinerProjectStatus.FAIL.getCode());
                purchaseMinerProject.setReason(esgResponse.getErrMessage());
                purchaseMinerProjectMapper.updateById(purchaseMinerProject);
                return esgResponse;
            }
        }
        return SingleResponse.buildSuccess();
    }

    @Override
    public MultiResponse<PurchaseMinerProjectDTO> page(PurchaseMinerProjectPageQry purchaseMinerProjectPageQry) {

        LambdaQueryWrapper<PurchaseMinerProject> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasLength(purchaseMinerProjectPageQry.getWalletAddress())) {
            lambdaQueryWrapper.eq(PurchaseMinerProject::getWalletAddress, purchaseMinerProjectPageQry.getWalletAddress());
        }

        Page<PurchaseMinerProject> purchaseMinerProjectPage = purchaseMinerProjectMapper.selectPage(Page.of(purchaseMinerProjectPageQry.getPageNum(), purchaseMinerProjectPageQry.getPageSize()), lambdaQueryWrapper);

        if (CollectionUtils.isEmpty(purchaseMinerProjectPage.getRecords())) {
            return MultiResponse.buildSuccess();
        }

        List<PurchaseMinerProjectDTO> purchaseMinerProjectDTOS = new ArrayList<>();

        for (PurchaseMinerProject purchaseMinerProject : purchaseMinerProjectPage.getRecords()) {
            PurchaseMinerProjectDTO purchaseMinerProjectDTO = new PurchaseMinerProjectDTO();
            BeanUtils.copyProperties(purchaseMinerProject, purchaseMinerProjectDTO);
            purchaseMinerProjectDTO.setTypeName(PurchaseMinerType.of(purchaseMinerProject.getType()).getName());
            purchaseMinerProjectDTO.setStatusName(PurchaseMinerProjectStatus.of(purchaseMinerProject.getStatus()).getName());
            purchaseMinerProjectDTOS.add(purchaseMinerProjectDTO);
        }
        return MultiResponse.of(purchaseMinerProjectDTOS, (int) purchaseMinerProjectPage.getTotal());
    }

    @Override
    public SingleResponse<Void> reward(PurchaseMinerProjectRewardCmd purchaseMinerProjectRewardCmd) {

        LambdaQueryWrapper<PurchaseMinerProject> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        List<PurchaseMinerProject> purchaseMinerProjectList = purchaseMinerProjectMapper.selectList(lambdaQueryWrapper);

        BigDecimal totalComputingPower = purchaseMinerProjectList
                .stream()
                .map(PurchaseMinerProject::getComputingPower)
                .map(BigDecimal::new)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        if (totalComputingPower.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("总算力为0，无法发放奖励");
            return SingleResponse.buildFailure("总算力为0");
        }

        LambdaQueryWrapper<MinerConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MinerConfig::getName, MinerConfigEnum.MINER_REQUIREMENT.getCode());

        MinerConfig minerConfig = minerConfigMapper.selectOne(queryWrapper);

        if (minerConfig == null || minerConfig.getValue() == null) {
            return SingleResponse.buildFailure("矿机算力要求配置错误");
        }

        BigDecimal minerRequirement = new BigDecimal(minerConfig.getValue());
        if (minerRequirement.compareTo(BigDecimal.ZERO) <= 0) {
            return SingleResponse.buildFailure("矿机算力要求配置错误");
        }
        if (totalComputingPower.compareTo(minerRequirement) < 0) {
            log.info("总算力{}未达到最小算力要求{}，无法发放奖励", totalComputingPower, minerRequirement);
            return SingleResponse.buildFailure("总算力未达到最小算力要求");
        }

        // 获取每天总奖励数
        LambdaQueryWrapper<SystemConfig> totalRewardQueryWrapper = new LambdaQueryWrapper<>();
        totalRewardQueryWrapper.eq(SystemConfig::getName, SystemConfigEnum.DAILY_TOTAL_REWARD.getCode());

        SystemConfig totalRewardSystemConfig = systemConfigMapper.selectOne(totalRewardQueryWrapper);
        if (totalRewardSystemConfig == null || totalRewardSystemConfig.getValue() == null) {
            return SingleResponse.buildFailure("每天挖矿总数系统配置错误");
        }

        BigDecimal totalReward = new BigDecimal(totalRewardSystemConfig.getValue());
        if (totalReward.compareTo(BigDecimal.ZERO) <= 0) {
            return SingleResponse.buildFailure("每天挖矿总数系统配置错误");
        }

        LambdaQueryWrapper<MinerConfig> minerAddNumberRequirementQueryWrapper = new LambdaQueryWrapper<>();
        minerAddNumberRequirementQueryWrapper.eq(MinerConfig::getName, MinerConfigEnum.MINER_ADD_NUMBER_REQUIREMENT.getCode());

        MinerConfig minerAddNumberRequirementConfig = minerConfigMapper.selectOne(minerAddNumberRequirementQueryWrapper);
        if (minerAddNumberRequirementConfig == null || minerAddNumberRequirementConfig.getValue() == null) {
            return SingleResponse.buildFailure("新增矿机挖矿数量要求配置错误");
        }
        BigDecimal minerAddNumberRequirement = new BigDecimal(minerAddNumberRequirementConfig.getValue());
        if (minerAddNumberRequirement.compareTo(BigDecimal.ZERO) <= 0) {
            return SingleResponse.buildFailure("新增矿机挖矿数量要求配置错误");
        }
        // 计算出多余的算力
        BigDecimal moreComputingPower = totalComputingPower.subtract(minerRequirement);

        // 计算出新增矿机数量
        if (moreComputingPower.compareTo(minerAddNumberRequirement) < 0) {
            log.info("新增算力{}未达到新增矿机挖矿数量要求{}，不增加挖矿数量", moreComputingPower, minerAddNumberRequirement);
        }else {
            // 新增挖矿数量的倍数 = 新增算力 / 新增矿机挖矿数量要求 （取余）
            BigDecimal times = moreComputingPower.divide(minerAddNumberRequirement, 0, BigDecimal.ROUND_DOWN);

            LambdaQueryWrapper<MinerConfig> minerAddNumberQueryWrapper = new LambdaQueryWrapper<>();
            minerAddNumberQueryWrapper.eq(MinerConfig::getName, MinerConfigEnum.MINER_ADD_NUMBER.getCode());

            MinerConfig minerAddNumberConfig = minerConfigMapper.selectOne(minerAddNumberQueryWrapper);
            if (minerAddNumberConfig == null || minerAddNumberConfig.getValue() == null) {
                return SingleResponse.buildFailure("新增矿机挖矿数量配置错误");
            }
            BigDecimal minerAddNumber = new BigDecimal(minerAddNumberConfig.getValue());
            if (minerAddNumber.compareTo(BigDecimal.ZERO) <= 0) {
                return SingleResponse.buildFailure("新增矿机挖矿数量配置错误");
            }

            BigDecimal totalAddNumber = minerAddNumber.multiply(times);
            log.info("新增算力{}达到新增矿机挖矿数量要求{}，增加挖矿数量{}", moreComputingPower, minerAddNumberRequirement, totalAddNumber);

            totalReward = totalReward.add(totalAddNumber);
        }


        LambdaQueryWrapper<SystemConfig> staticQueryWrapper = new LambdaQueryWrapper<>();
        staticQueryWrapper.eq(SystemConfig::getName, SystemConfigEnum.STATIC_REWARD_RATE.getCode());

        SystemConfig staticSystemConfig = systemConfigMapper.selectOne(staticQueryWrapper);
        if (staticSystemConfig == null || staticSystemConfig.getValue() == null) {
            return SingleResponse.buildFailure("静态奖励系统配置错误");
        }

        BigDecimal staticRewardRate = new BigDecimal(staticSystemConfig.getValue());
        if (staticRewardRate.compareTo(BigDecimal.ZERO) < 0 || staticRewardRate.compareTo(new BigDecimal(1)) > 0) {
            return SingleResponse.buildFailure("静态奖励系统配置错误");
        }



        return null;
    }

    /**
     *
     * 获取每个矿机的静态奖励
     */
    private PurchaseMinerProjectReward getStaticReward(PurchaseMinerProject purchaseMinerProject,BigDecimal staticRewardRate,BigDecimal totalComputingPower){


    }

    /**
     * 获取每日总奖励
     */
    private BigDecimal getTotalReward(){

    }
}
