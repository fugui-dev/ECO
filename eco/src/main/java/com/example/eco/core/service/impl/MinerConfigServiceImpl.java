package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.MinerConfigCreateCmd;
import com.example.eco.bean.cmd.MinerConfigUpdateCmd;
import com.example.eco.bean.dto.MinerConfigDTO;
import com.example.eco.core.service.MinerConfigService;
import com.example.eco.model.entity.MinerConfig;
import com.example.eco.model.mapper.MinerConfigMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class MinerConfigServiceImpl implements MinerConfigService {

    @Resource
    private MinerConfigMapper minerConfigMapper;


    @Override
    public SingleResponse<Void> update(MinerConfigUpdateCmd minerConfigUpdateCmd) {

        LambdaQueryWrapper<MinerConfig> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(MinerConfig::getName, minerConfigUpdateCmd.getName());

        MinerConfig existingConfig = minerConfigMapper.selectOne(lambdaQueryWrapper);

        if (existingConfig != null) {
            existingConfig.setValue(minerConfigUpdateCmd.getValue());
            minerConfigMapper.updateById(existingConfig);
            return SingleResponse.buildSuccess();
        } else {
            MinerConfig newConfig = new MinerConfig();
            newConfig.setName(minerConfigUpdateCmd.getName());
            newConfig.setValue(minerConfigUpdateCmd.getValue());
            minerConfigMapper.insert(newConfig);
            return SingleResponse.buildSuccess();
        }

    }

    @Override
    public MultiResponse<MinerConfigDTO> list() {

        List<MinerConfig> minerConfigList = minerConfigMapper.selectList(new LambdaQueryWrapper<>());

        if (CollectionUtils.isEmpty(minerConfigList)) {
            return MultiResponse.buildSuccess();
        }

        List<MinerConfigDTO> minerConfigDTOList = new ArrayList<>();

        for (MinerConfig minerConfig : minerConfigList) {
            MinerConfigDTO minerConfigDTO = new MinerConfigDTO();
            BeanUtils.copyProperties(minerConfig, minerConfigDTO);
            minerConfigDTOList.add(minerConfigDTO);
        }
        return MultiResponse.of(minerConfigDTOList);
    }
}
