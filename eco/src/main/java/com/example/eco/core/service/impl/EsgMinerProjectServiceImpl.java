package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.bean.dto.EsgMinerProjectDTO;
import com.example.eco.bean.dto.MinerProjectDTO;
import com.example.eco.core.service.EsgMinerProjectService;
import com.example.eco.core.service.MinerProjectService;
import com.example.eco.model.entity.EsgMinerProject;
import com.example.eco.model.entity.EsgPurchaseMinerProject;
import com.example.eco.model.entity.MinerProject;
import com.example.eco.model.entity.MinerProjectStatisticsLog;
import com.example.eco.model.mapper.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class EsgMinerProjectServiceImpl implements EsgMinerProjectService {

    @Resource
    private EsgMinerProjectMapper esgMinerProjectMapper;
    @Resource
    private EsgPurchaseMinerProjectMapper esgPurchaseMinerProjectMapper;


    @Override
    public SingleResponse<Void> create(EsgMinerProjectCreateCmd esgMinerProjectCreateCmd) {

        LambdaQueryWrapper<EsgMinerProject> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(EsgMinerProject::getComputingPower, esgMinerProjectCreateCmd.getComputingPower());
        lambdaQueryWrapper.eq(EsgMinerProject::getPrice, esgMinerProjectCreateCmd.getPrice());

        EsgMinerProject existingProject = esgMinerProjectMapper.selectOne(lambdaQueryWrapper);
        if (existingProject != null) {
            return SingleResponse.buildFailure("矿机项目已存在");
        }

        existingProject = new EsgMinerProject();
        existingProject.setComputingPower(esgMinerProjectCreateCmd.getComputingPower());
        existingProject.setPrice(esgMinerProjectCreateCmd.getPrice());
        existingProject.setStatus(1);
        existingProject.setRate(esgMinerProjectCreateCmd.getRate());
        existingProject.setCreateTime(System.currentTimeMillis());
        esgMinerProjectMapper.insert(existingProject);
        return SingleResponse.buildSuccess();
    }

    @Override
    public SingleResponse<Void> update(EsgMinerProjectUpdateCmd esgMinerProjectUpdateCmd) {

        EsgMinerProject minerProject = esgMinerProjectMapper.selectById(esgMinerProjectUpdateCmd.getId());
        if (minerProject == null) {
            return SingleResponse.buildFailure("矿机项目不存在");
        }

        minerProject.setComputingPower(esgMinerProjectUpdateCmd.getComputingPower());
        minerProject.setPrice(esgMinerProjectUpdateCmd.getPrice());
        minerProject.setRate(esgMinerProjectUpdateCmd.getRate());
        minerProject.setStatus(esgMinerProjectUpdateCmd.getStatus());
        minerProject.setUpdateTime(System.currentTimeMillis());
        esgMinerProjectMapper.updateById(minerProject);
        return SingleResponse.buildSuccess();
    }

    @Override
    public SingleResponse<Void> delete(MinerProjectDeleteCmd minerProjectDeleteCmd) {

        LambdaQueryWrapper<EsgPurchaseMinerProject> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(EsgPurchaseMinerProject::getMinerProjectId,minerProjectDeleteCmd.getId());

        Long count = esgPurchaseMinerProjectMapper.selectCount(lambdaQueryWrapper);
        if (count > 0){
            return SingleResponse.buildFailure("用户已购买过该类型矿机，不能删除");
        }

        esgMinerProjectMapper.deleteById(minerProjectDeleteCmd.getId());
        return SingleResponse.buildSuccess();
    }

    @Override
    public MultiResponse<EsgMinerProjectDTO> page(MinerProjectPageQry minerProjectPageQry) {

        LambdaQueryWrapper<EsgMinerProject> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Objects.nonNull(minerProjectPageQry.getStatus()),EsgMinerProject::getStatus,minerProjectPageQry.getStatus());

        Page<EsgMinerProject> minerProjectPage = esgMinerProjectMapper.selectPage(Page.of(minerProjectPageQry.getPageNum(), minerProjectPageQry.getPageSize()), lambdaQueryWrapper);
        if (CollectionUtils.isEmpty(minerProjectPage.getRecords())) {
            return MultiResponse.buildSuccess();
        }


        List<EsgMinerProjectDTO> minerProjectDTOList = new ArrayList<>();

        for (EsgMinerProject minerProject : minerProjectPage.getRecords()) {

            EsgMinerProjectDTO minerProjectDTO = new EsgMinerProjectDTO();
            minerProjectDTO.setId(minerProject.getId());
            minerProjectDTO.setPrice(minerProject.getPrice());
            minerProjectDTO.setStatus(minerProject.getStatus());
            minerProjectDTO.setComputingPower(minerProject.getComputingPower());
            minerProjectDTO.setRate(minerProject.getRate());
            minerProjectDTO.setCreateTime(minerProject.getCreateTime());
            minerProjectDTO.setUpdateTime(minerProject.getUpdateTime());

            minerProjectDTOList.add(minerProjectDTO);

        }

        return MultiResponse.of(minerProjectDTOList, (int) minerProjectPage.getTotal());
    }

}
