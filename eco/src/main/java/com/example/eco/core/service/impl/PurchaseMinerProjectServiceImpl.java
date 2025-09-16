package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.bean.dto.PurchaseMinerProjectDTO;
import com.example.eco.common.*;
import com.example.eco.core.service.AccountService;
import com.example.eco.core.service.MinerProjectService;
import com.example.eco.core.service.PurchaseMinerProjectService;
import com.example.eco.core.service.RecommendStatisticsLogService;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    private RecommendStatisticsLogService recommendStatisticsLogService;
    @Resource
    private MinerProjectService minerProjectService;
    @Resource
    private MinerProjectStatisticsLogMapper minerProjectStatisticsLogMapper;
    @Resource
    private SystemConfigMapper systemConfigMapper;


    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> create(PurchaseMinerProjectsCreateCmd purchaseMinerProjectsCreateCmd) {

        MinerProject minerProject = minerProjectMapper.selectById(purchaseMinerProjectsCreateCmd.getMinerProjectId());
        if (minerProject == null) {
            return SingleResponse.buildFailure("矿机不存在");
        }

        String dayTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Boolean checkQuota = checkQuota(minerProject, dayTime);
        if (!checkQuota) {
            return SingleResponse.buildFailure("该矿机ESG已达限额,请使用ECO支付");
        }

        // todo U -> 转ESG ECO

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

        BigDecimal amount = BigDecimal.ZERO;

        if (purchaseMinerProjectsCreateCmd.getType().equals(PurchaseMinerType.ESG.getCode())) {

            AccountDeductCmd accountDeductCmd = new AccountDeductCmd();
            accountDeductCmd.setAccountType(AccountType.ESG.getCode());
            accountDeductCmd.setNumber(purchaseMinerProjectsCreateCmd.getEsgNumber());
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

                amount = amount.add(new BigDecimal(minerProject.getPrice()));

            }
        }

        if (purchaseMinerProjectsCreateCmd.getType().equals(PurchaseMinerType.ECO.getCode())) {

            LambdaQueryWrapper<SystemConfig> systemConfigLambdaQueryWrapper = new LambdaQueryWrapper<>();
            systemConfigLambdaQueryWrapper.eq(SystemConfig::getName, SystemConfigEnum.ECO_PRICE.getCode());

            SystemConfig systemConfig = systemConfigMapper.selectOne(systemConfigLambdaQueryWrapper);
            if (Objects.isNull(systemConfig)) {
                return SingleResponse.buildFailure("未设置ECO价格");
            }

            BigDecimal ecoNumber = new BigDecimal(systemConfig.getValue()).multiply(new BigDecimal(minerProject.getPrice()));

            purchaseMinerProject.setEcoNumber(ecoNumber.toString());

            AccountDeductCmd accountDeductCmd = new AccountDeductCmd();
            accountDeductCmd.setAccountType(AccountType.ECO.getCode());
            accountDeductCmd.setNumber(ecoNumber.toString());
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

            }
        }

        if (purchaseMinerProjectsCreateCmd.getType().equals(PurchaseMinerType.ECO_ESG.getCode())) {

            BigDecimal halfPrice = new BigDecimal(minerProject.getPrice()).divide(new BigDecimal(2), 4, RoundingMode.HALF_DOWN);

            LambdaQueryWrapper<SystemConfig> systemConfigLambdaQueryWrapper = new LambdaQueryWrapper<>();
            systemConfigLambdaQueryWrapper.eq(SystemConfig::getName, SystemConfigEnum.ECO_PRICE.getCode());
            SystemConfig systemConfig = systemConfigMapper.selectOne(systemConfigLambdaQueryWrapper);
            if (Objects.isNull(systemConfig)) {
                return SingleResponse.buildFailure("未设置ECO价格");
            }

            BigDecimal ecoNumber = new BigDecimal(systemConfig.getValue()).multiply(halfPrice);

            purchaseMinerProject.setEcoNumber(ecoNumber.toString());

            AccountDeductCmd ecoAccountDeductCmd = new AccountDeductCmd();
            ecoAccountDeductCmd.setAccountType(AccountType.ECO.getCode());
            ecoAccountDeductCmd.setNumber(ecoNumber.toString());
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
            esgAccountDeductCmd.setAccountType(AccountType.ESG.getCode());
            esgAccountDeductCmd.setNumber(purchaseMinerProjectsCreateCmd.getEcoNumber());
            esgAccountDeductCmd.setWalletAddress(purchaseMinerProjectsCreateCmd.getWalletAddress());
            esgAccountDeductCmd.setOrder(order);
            SingleResponse<Void> esgResponse = accountService.purchaseMinerProjectNumber(esgAccountDeductCmd);
            if (!esgResponse.isSuccess()) {

                purchaseMinerProject.setStatus(PurchaseMinerProjectStatus.FAIL.getCode());
                purchaseMinerProject.setReason(esgResponse.getErrMessage());
                purchaseMinerProjectMapper.updateById(purchaseMinerProject);
                return esgResponse;
            }

            amount = amount.add(halfPrice);
        }

        TotalComputingPowerCmd totalComputingPowerCmd = new TotalComputingPowerCmd();
        totalComputingPowerCmd.setWalletAddress(purchaseMinerProjectsCreateCmd.getWalletAddress());
        totalComputingPowerCmd.setComputingPower(minerProject.getComputingPower());

        recommendStatisticsLogService.statistics(totalComputingPowerCmd);


        if (amount.compareTo(BigDecimal.ZERO) == 0) {

            MinerProjectStatisticsLogCmd minerProjectStatisticsLogCmd = new MinerProjectStatisticsLogCmd();
            minerProjectStatisticsLogCmd.setAmount(amount);
            minerProjectStatisticsLogCmd.setMinerProjectId(minerProject.getId());
            minerProjectStatisticsLogCmd.setDayTime(dayTime);
            minerProjectStatisticsLogCmd.setEsgNumber(new BigDecimal(purchaseMinerProjectsCreateCmd.getEsgNumber()));

            minerProjectService.statistics(minerProjectStatisticsLogCmd);
        }

        return SingleResponse.buildSuccess();
    }


    /**
     * 检查限额
     */
    public Boolean checkQuota(MinerProject minerProject, String dayTime) {

        LambdaQueryWrapper<MinerProjectStatisticsLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MinerProjectStatisticsLog::getDayTime, dayTime);
        queryWrapper.eq(MinerProjectStatisticsLog::getMinerProjectId, minerProject.getId());

        MinerProjectStatisticsLog minerProjectStatisticsLog = minerProjectStatisticsLogMapper.selectOne(queryWrapper);

        if (Objects.isNull(minerProjectStatisticsLog)) {
            return Boolean.TRUE;
        }

        BigDecimal price = new BigDecimal(minerProject.getPrice());

        BigDecimal halfPrice = price.divide(new BigDecimal(minerProject.getPrice()).divide(new BigDecimal(2), 4, RoundingMode.HALF_DOWN));

        BigDecimal remaining = new BigDecimal(minerProject.getQuota()).subtract(new BigDecimal(minerProjectStatisticsLog.getAmount()));


        if (remaining.compareTo(price) < 0 || remaining.compareTo(halfPrice) < 0) {
            return Boolean.FALSE;
        } else {
            return Boolean.TRUE;
        }
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
}
