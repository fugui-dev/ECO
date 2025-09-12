package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.MinerProjectCreateCmd;
import com.example.eco.bean.cmd.MinerProjectDeleteCmd;
import com.example.eco.bean.cmd.MinerProjectPageQry;
import com.example.eco.bean.cmd.MinerProjectUpdateCmd;
import com.example.eco.bean.dto.MinerProjectDTO;
import com.example.eco.common.SystemConfigEnum;
import com.example.eco.core.service.MinerProjectService;
import com.example.eco.model.entity.MinerProject;
import com.example.eco.model.entity.SystemConfig;
import com.example.eco.model.mapper.MinerProjectMapper;
import com.example.eco.model.mapper.SystemConfigMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MinerProjectServiceImpl implements MinerProjectService {

    @Resource
    private MinerProjectMapper minerProjectMapper;
    @Resource
    private SystemConfigMapper systemConfigMapper;
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

        LambdaQueryWrapper<SystemConfig> systemConfigLambdaQueryWrapper = new LambdaQueryWrapper<>();
        systemConfigLambdaQueryWrapper.eq(SystemConfig::getName, SystemConfigEnum.STATIC_NEW_MINER_RATE.getCode());
        SystemConfig systemConfig = systemConfigMapper.selectOne(systemConfigLambdaQueryWrapper);

        if (systemConfig == null || systemConfig.getValue() == null) {
            return MultiResponse.buildFailure("405","新增算力系统配置错误");
        }

        List<MinerProjectDTO> minerProjectDTOList = new ArrayList<>();
        for (MinerProject minerProject : minerProjectPage.getRecords()) {
            MinerProjectDTO minerProjectDTO = new MinerProjectDTO();
            minerProjectDTO.setId(minerProject.getId());
            minerProjectDTO.setPrice(minerProject.getPrice());

            Long days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - minerProject.getCreateTime());
            Double computingPower = Double.parseDouble(minerProject.getComputingPower()) * Math.pow(Double.parseDouble(systemConfig.getValue()), days);
            minerProjectDTO.setComputingPower(computingPower.toString());
            minerProjectDTOList.add(minerProjectDTO);
        }

        return MultiResponse.of(minerProjectDTOList, (int) minerProjectPage.getTotal());
    }
}
