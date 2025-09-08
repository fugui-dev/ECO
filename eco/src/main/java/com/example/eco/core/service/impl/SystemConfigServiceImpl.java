package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.SystemConfigUpdateCmd;
import com.example.eco.bean.dto.SystemConfigDTO;
import com.example.eco.core.service.SystemConfigService;
import com.example.eco.model.entity.SystemConfig;
import com.example.eco.model.mapper.SystemConfigMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    @Resource
    private SystemConfigMapper systemConfigMapper;

    @Override
    public SingleResponse<Void> update(SystemConfigUpdateCmd systemConfigUpdateCmd) {

        LambdaQueryWrapper<SystemConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemConfig::getName, systemConfigUpdateCmd.getName());

        SystemConfig systemConfig = systemConfigMapper.selectOne(queryWrapper);
        if (systemConfig != null) {
            systemConfig.setValue(systemConfigUpdateCmd.getValue());
            systemConfigMapper.updateById(systemConfig);
        } else {
            systemConfig = SystemConfig.builder()
                    .name(systemConfigUpdateCmd.getName())
                    .value(systemConfigUpdateCmd.getValue())
                    .build();
            systemConfigMapper.insert(systemConfig);
        }

        return SingleResponse.buildSuccess();
    }

    @Override
    public MultiResponse<SystemConfigDTO> list() {

        LambdaQueryWrapper<SystemConfig> queryWrapper = new LambdaQueryWrapper<>();

        List<SystemConfig> systemConfigList = systemConfigMapper.selectList(queryWrapper);

        if (CollectionUtils.isEmpty(systemConfigList)) {
            return MultiResponse.buildSuccess();
        }

        List<SystemConfigDTO> systemConfigDTOList = new ArrayList<>();
        for (SystemConfig systemConfig : systemConfigList){
            SystemConfigDTO systemConfigDTO = new SystemConfigDTO();
            systemConfigDTO.setName(systemConfig.getName());
            systemConfigDTO.setValue(systemConfig.getValue());
            systemConfigDTOList.add(systemConfigDTO);
        }
        return MultiResponse.of(systemConfigDTOList);
    }
}
