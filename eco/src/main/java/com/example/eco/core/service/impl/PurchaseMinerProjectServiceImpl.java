package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.AccountDeductCmd;
import com.example.eco.bean.cmd.PurchaseMinerProjectPageQry;
import com.example.eco.bean.cmd.PurchaseMinerProjectsCreateCmd;
import com.example.eco.bean.dto.PurchaseMinerProjectDTO;
import com.example.eco.common.AccountType;
import com.example.eco.common.PurchaseMinerProjectStatus;
import com.example.eco.common.PurchaseMinerType;
import com.example.eco.common.SystemConfigEnum;
import com.example.eco.core.service.AccountService;
import com.example.eco.core.service.PurchaseMinerProjectService;
import com.example.eco.model.entity.Account;
import com.example.eco.model.entity.MinerProject;
import com.example.eco.model.entity.PurchaseMinerProject;
import com.example.eco.model.entity.SystemConfig;
import com.example.eco.model.mapper.AccountMapper;
import com.example.eco.model.mapper.MinerProjectMapper;
import com.example.eco.model.mapper.PurchaseMinerProjectMapper;
import com.example.eco.model.mapper.SystemConfigMapper;
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

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> create(PurchaseMinerProjectsCreateCmd purchaseMinerProjectsCreateCmd) {

        MinerProject minerProject = minerProjectMapper.selectById(purchaseMinerProjectsCreateCmd.getMinerProjectId());
        if (minerProject == null) {
            return SingleResponse.buildFailure("矿机不存在");
        }

        //todo 计算天数 (当前时间-矿机创建时间)/天数
        Long days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - minerProject.getCreateTime());
        //todo 根据天数 算出实际到账算力
        LambdaQueryWrapper<SystemConfig> systemConfigLambdaQueryWrapper = new LambdaQueryWrapper<>();
        systemConfigLambdaQueryWrapper.eq(SystemConfig::getName, SystemConfigEnum.STATIC_NEW_MINER_RATE.getCode());
        SystemConfig systemConfig = systemConfigMapper.selectOne(systemConfigLambdaQueryWrapper);

        if (systemConfig == null || systemConfig.getValue() == null) {
            return SingleResponse.buildFailure("新增算力系统配置错误");
        }

        String order = "PMP" + System.currentTimeMillis();

        Double computingPower = Double.parseDouble(minerProject.getComputingPower()) * Math.pow(Double.parseDouble(systemConfig.getValue()), days);

        PurchaseMinerProject purchaseMinerProject = new PurchaseMinerProject();
        purchaseMinerProject.setMinerProjectId(purchaseMinerProjectsCreateCmd.getMinerProjectId());
        purchaseMinerProject.setPrice(minerProject.getPrice());
        purchaseMinerProject.setComputingPower(computingPower.toString());
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
                purchaseMinerProjectMapper.updateById(purchaseMinerProject);
                return response;
            } else {
                // 购买成功，记录购买信息
                purchaseMinerProject.setStatus(PurchaseMinerProjectStatus.SUCCESS.getCode());
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
}
