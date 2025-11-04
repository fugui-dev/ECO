package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.AccountDeductCmd;
import com.example.eco.bean.cmd.EsgPurchaseMinerProjectsCreateCmd;
import com.example.eco.bean.cmd.PurchaseMinerProjectPageQry;
import com.example.eco.bean.dto.EsgPurchaseMinerProjectDTO;
import com.example.eco.bean.dto.PurchaseMinerProjectDTO;
import com.example.eco.common.PurchaseMinerProjectStatus;
import com.example.eco.common.PurchaseMinerType;
import com.example.eco.core.service.EsgAccountService;
import com.example.eco.core.service.EsgPurchaseMinerProjectService;
import com.example.eco.model.entity.*;
import com.example.eco.model.mapper.EsgMinerProjectMapper;
import com.example.eco.model.mapper.EsgPurchaseMinerProjectMapper;
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

@Service
public class EsgPurchaseMinerProjectServiceImpl implements EsgPurchaseMinerProjectService {

    @Resource
    private EsgMinerProjectMapper esgMinerProjectMapper;

    @Resource
    private EsgPurchaseMinerProjectMapper esgPurchaseMinerProjectMapper;

    @Resource
    private EsgAccountService esgAccountService;

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> create(EsgPurchaseMinerProjectsCreateCmd esgPurchaseMinerProjectsCreateCmd) {

        EsgMinerProject minerProject = esgMinerProjectMapper.selectById(esgPurchaseMinerProjectsCreateCmd.getMinerProjectId());
        if (minerProject == null) {
            return SingleResponse.buildFailure("矿机不存在");
        }

        if (minerProject.getStatus() != 1) {
            return SingleResponse.buildFailure("矿机不允许购买");
        }


        String order = "EPMP" + System.currentTimeMillis();


        AccountDeductCmd accountDeductCmd = new AccountDeductCmd();
        accountDeductCmd.setWalletAddress(esgPurchaseMinerProjectsCreateCmd.getWalletAddress());
        accountDeductCmd.setNumber(minerProject.getPrice());
        accountDeductCmd.setOrder(order);
        SingleResponse<Void> deductResponse = esgAccountService.purchaseMinerProjectNumber(accountDeductCmd);

        if (!deductResponse.isSuccess()) {
            return deductResponse;
        }

        EsgPurchaseMinerProject esgPurchaseMinerProject = new EsgPurchaseMinerProject();
        esgPurchaseMinerProject.setOrder(order);
        esgPurchaseMinerProject.setMinerProjectId(minerProject.getId());
        esgPurchaseMinerProject.setPrice(minerProject.getPrice());
        esgPurchaseMinerProject.setComputingPower(minerProject.getComputingPower());
        esgPurchaseMinerProject.setWalletAddress(esgPurchaseMinerProjectsCreateCmd.getWalletAddress());
        esgPurchaseMinerProject.setStatus(PurchaseMinerProjectStatus.SUCCESS.getCode());
        esgPurchaseMinerProject.setFinishTime(System.currentTimeMillis());
        esgPurchaseMinerProject.setCreateTime(System.currentTimeMillis());
        esgPurchaseMinerProject.setReward("0");
        esgPurchaseMinerProject.setYesterdayReward("0");

        esgPurchaseMinerProjectMapper.insert(esgPurchaseMinerProject);

        return SingleResponse.buildSuccess();

    }

    @Override
    public MultiResponse<EsgPurchaseMinerProjectDTO> page(PurchaseMinerProjectPageQry purchaseMinerProjectPageQry) {
        LambdaQueryWrapper<EsgPurchaseMinerProject> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(StringUtils.hasLength(purchaseMinerProjectPageQry.getWalletAddress()), EsgPurchaseMinerProject::getWalletAddress, purchaseMinerProjectPageQry.getWalletAddress());
        lambdaQueryWrapper.eq(StringUtils.hasLength(purchaseMinerProjectPageQry.getStatus()), EsgPurchaseMinerProject::getStatus, purchaseMinerProjectPageQry.getStatus());

        if (Objects.nonNull(purchaseMinerProjectPageQry.getStartTime()) && Objects.nonNull(purchaseMinerProjectPageQry.getEndTime())) {
            lambdaQueryWrapper.between(EsgPurchaseMinerProject::getCreateTime, purchaseMinerProjectPageQry.getStartTime(), purchaseMinerProjectPageQry.getEndTime());
        }

        Page<EsgPurchaseMinerProject> purchaseMinerProjectPage = esgPurchaseMinerProjectMapper.selectPage(Page.of(purchaseMinerProjectPageQry.getPageNum(), purchaseMinerProjectPageQry.getPageSize()), lambdaQueryWrapper);

        if (CollectionUtils.isEmpty(purchaseMinerProjectPage.getRecords())) {
            return MultiResponse.buildSuccess();
        }

        List<EsgPurchaseMinerProjectDTO> purchaseMinerProjectDTOS = new ArrayList<>();


        for (EsgPurchaseMinerProject purchaseMinerProject : purchaseMinerProjectPage.getRecords()) {
            EsgPurchaseMinerProjectDTO purchaseMinerProjectDTO = new EsgPurchaseMinerProjectDTO();
            BeanUtils.copyProperties(purchaseMinerProject, purchaseMinerProjectDTO);
            purchaseMinerProjectDTO.setStatusName(PurchaseMinerProjectStatus.of(purchaseMinerProject.getStatus()).getName());
            purchaseMinerProjectDTOS.add(purchaseMinerProjectDTO);
        }
        return MultiResponse.of(purchaseMinerProjectDTOS, (int) purchaseMinerProjectPage.getTotal());
    }
}
