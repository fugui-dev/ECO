package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.bean.dto.MinerProjectDTO;
import com.example.eco.common.SystemConfigEnum;
import com.example.eco.core.service.MinerProjectService;
import com.example.eco.model.entity.MinerProject;
import com.example.eco.model.entity.MinerProjectStatisticsLog;
import com.example.eco.model.entity.SystemConfig;
import com.example.eco.model.mapper.MinerProjectMapper;
import com.example.eco.model.mapper.MinerProjectStatisticsLogMapper;
import com.example.eco.model.mapper.SystemConfigMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class MinerProjectServiceImpl implements MinerProjectService {

    @Resource
    private MinerProjectMapper minerProjectMapper;
    @Resource
    private SystemConfigMapper systemConfigMapper;
    @Resource
    private MinerProjectStatisticsLogMapper minerProjectStatisticsLogMapper;

    @Override
    public SingleResponse<Void> create(MinerProjectCreateCmd minerProjectCreateCmd) {

        LambdaQueryWrapper<MinerProject> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(MinerProject::getComputingPower, minerProjectCreateCmd.getComputingPower());
        lambdaQueryWrapper.eq(MinerProject::getPrice, minerProjectCreateCmd.getPrice());

        MinerProject existingProject = minerProjectMapper.selectOne(lambdaQueryWrapper);
        if (existingProject != null) {
            return SingleResponse.buildFailure("矿机项目已存在");
        }

        existingProject = new MinerProject();
        existingProject.setComputingPower(minerProjectCreateCmd.getComputingPower());
        existingProject.setPrice(minerProjectCreateCmd.getPrice());
        existingProject.setQuota(minerProjectCreateCmd.getQuota());
        existingProject.setCreateTime(System.currentTimeMillis());
        minerProjectMapper.insert(existingProject);
        return SingleResponse.buildSuccess();
    }

    @Override
    public SingleResponse<Void> update(MinerProjectUpdateCmd minerProjectUpdateCmd) {

        MinerProject minerProject = minerProjectMapper.selectById(minerProjectUpdateCmd.getId());
        if (minerProject == null) {
            return SingleResponse.buildFailure("矿机项目不存在");
        }

        minerProject.setComputingPower(minerProjectUpdateCmd.getComputingPower());
        minerProject.setPrice(minerProjectUpdateCmd.getPrice());
        minerProject.setQuota(minerProjectUpdateCmd.getQuota());
        minerProject.setUpdateTime(System.currentTimeMillis());
        minerProjectMapper.updateById(minerProject);
        return SingleResponse.buildSuccess();
    }

    @Override
    public SingleResponse<Void> delete(MinerProjectDeleteCmd minerProjectDeleteCmd) {
        minerProjectMapper.deleteById(minerProjectDeleteCmd.getId());
        return SingleResponse.buildSuccess();
    }

    @Override
    public MultiResponse<MinerProjectDTO> page(MinerProjectPageQry minerProjectPageQry) {

        LambdaQueryWrapper<MinerProject> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        Page<MinerProject> minerProjectPage = minerProjectMapper.selectPage(Page.of(minerProjectPageQry.getPageNum(), minerProjectPageQry.getPageSize()), lambdaQueryWrapper);
        if (CollectionUtils.isEmpty(minerProjectPage.getRecords())) {
            return MultiResponse.buildSuccess();
        }

//        LambdaQueryWrapper<SystemConfig> systemConfigLambdaQueryWrapper = new LambdaQueryWrapper<>();
//        systemConfigLambdaQueryWrapper.eq(SystemConfig::getName, SystemConfigEnum.STATIC_NEW_MINER_RATE.getCode());
//        SystemConfig systemConfig = systemConfigMapper.selectOne(systemConfigLambdaQueryWrapper);
//
//        if (systemConfig == null || systemConfig.getValue() == null) {
//            return MultiResponse.buildFailure("405","新增算力系统配置错误");
//        }

        String dayTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        List<MinerProjectDTO> minerProjectDTOList = new ArrayList<>();
        for (MinerProject minerProject : minerProjectPage.getRecords()) {

            MinerProjectDTO minerProjectDTO = new MinerProjectDTO();
            minerProjectDTO.setId(minerProject.getId());
            minerProjectDTO.setPrice(minerProject.getPrice());
            minerProjectDTO.setQuota(minerProject.getQuota());
//            Long days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - minerProject.getCreateTime());
//            Double computingPower = Double.parseDouble(minerProject.getComputingPower()) * Math.pow(Double.parseDouble(systemConfig.getValue()), days);


            minerProjectDTO.setComputingPower(minerProject.getComputingPower());

            LambdaQueryWrapper<MinerProjectStatisticsLog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(MinerProjectStatisticsLog::getDayTime, dayTime);
            queryWrapper.eq(MinerProjectStatisticsLog::getMinerProjectId, minerProject.getId());

            MinerProjectStatisticsLog minerProjectStatisticsLog = minerProjectStatisticsLogMapper.selectOne(queryWrapper);
            if (Objects.isNull(minerProjectStatisticsLog)) {
                minerProjectDTO.setAmount("0");
            } else {
                minerProjectDTO.setAmount(minerProjectStatisticsLog.getAmount());
            }

            BigDecimal price = new BigDecimal(minerProject.getPrice());

            BigDecimal halfPrice = price.divide(new BigDecimal(minerProject.getPrice()).divide(new BigDecimal(2), 4, RoundingMode.HALF_DOWN));

            BigDecimal remaining = new BigDecimal(minerProject.getQuota()).subtract(new BigDecimal(minerProjectDTO.getAmount()));

            if (remaining.compareTo(price) < 0 || remaining.compareTo(halfPrice) < 0) {
                minerProjectDTO.setDisable(Boolean.TRUE);
            }else {
                minerProjectDTO.setDisable(Boolean.FALSE);
            }

            minerProjectDTOList.add(minerProjectDTO);

        }

        return MultiResponse.of(minerProjectDTOList, (int) minerProjectPage.getTotal());
    }

    @Override
    public SingleResponse<Void> statistics(MinerProjectStatisticsLogCmd minerProjectStatisticsLogCmd) {

        LambdaQueryWrapper<MinerProjectStatisticsLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MinerProjectStatisticsLog::getDayTime, minerProjectStatisticsLogCmd.getDayTime());

        MinerProjectStatisticsLog minerProjectStatisticsLog = minerProjectStatisticsLogMapper.selectOne(queryWrapper);
        if (Objects.isNull(minerProjectStatisticsLog)) {
            minerProjectStatisticsLog = new MinerProjectStatisticsLog();
            minerProjectStatisticsLog.setDayTime(minerProjectStatisticsLogCmd.getDayTime());
            minerProjectStatisticsLog.setAmount("0");
            minerProjectStatisticsLog.setEsgNumber("0");
        }

        BigDecimal amount = new BigDecimal(minerProjectStatisticsLog.getAmount()).add(minerProjectStatisticsLogCmd.getAmount());

        BigDecimal esgNumber = new BigDecimal(minerProjectStatisticsLog.getEsgNumber()).add(minerProjectStatisticsLogCmd.getEsgNumber());

        minerProjectStatisticsLog.setEsgNumber(esgNumber.toString());
        minerProjectStatisticsLog.setAmount(amount.toString());

        if (Objects.isNull(minerProjectStatisticsLog.getId())) {
            minerProjectStatisticsLogMapper.insert(minerProjectStatisticsLog);
        } else {
            minerProjectStatisticsLogMapper.updateById(minerProjectStatisticsLog);
        }
        return SingleResponse.buildSuccess();
    }
}
