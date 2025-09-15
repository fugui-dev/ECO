package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.bean.dto.PurchaseMinerProjectDTO;
import com.example.eco.common.*;
import com.example.eco.core.service.AccountService;
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
import java.util.ArrayList;
import java.util.List;

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
    @Resource
    private RecommendMapper recommendMapper;
    @Resource
    private PurchaseMinerProjectRewardMapper purchaseMinerProjectRewardMapper;
    @Resource
    private RecommendStatisticsLogService recommendStatisticsLogService;


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
            ecoAccountDeductCmd.setNumber(new BigDecimal(minerProject.getPrice()).divide(new BigDecimal(2), 4, RoundingMode.HALF_DOWN).toString());
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
            esgAccountDeductCmd.setNumber(new BigDecimal(minerProject.getPrice()).divide(new BigDecimal(2), 4, RoundingMode.HALF_DOWN).toString());
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

        TotalComputingPowerCmd totalComputingPowerCmd = new TotalComputingPowerCmd();
        totalComputingPowerCmd.setWalletAddress(purchaseMinerProjectsCreateCmd.getWalletAddress());
        totalComputingPowerCmd.setComputingPower(minerProject.getComputingPower());

        recommendStatisticsLogService.statistics(totalComputingPowerCmd);

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
}
