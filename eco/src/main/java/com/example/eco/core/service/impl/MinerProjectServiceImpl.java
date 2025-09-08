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
import com.example.eco.core.service.MinerProjectService;
import com.example.eco.model.entity.MinerProject;
import com.example.eco.model.mapper.MinerProjectMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class MinerProjectServiceImpl implements MinerProjectService {

    @Resource
    private MinerProjectMapper minerProjectMapper;

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

        List<MinerProjectDTO> minerProjectDTOList = new ArrayList<>();
        for (MinerProject minerProject : minerProjectPage.getRecords()) {
            MinerProjectDTO minerProjectDTO = new MinerProjectDTO();
            minerProjectDTO.setId(minerProject.getId());
            minerProjectDTO.setComputingPower(minerProject.getComputingPower());
            minerProjectDTO.setPrice(minerProject.getPrice());
            minerProjectDTOList.add(minerProjectDTO);
        }

        return MultiResponse.of(minerProjectDTOList, (int) minerProjectPage.getTotal());
    }
}
