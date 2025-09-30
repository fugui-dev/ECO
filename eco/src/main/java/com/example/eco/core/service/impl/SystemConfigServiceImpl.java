package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.SystemConfigLogPageQry;
import com.example.eco.bean.cmd.SystemConfigUpdateCmd;
import com.example.eco.bean.dto.SystemConfigDTO;
import com.example.eco.bean.dto.SystemConfigLogDTO;
import com.example.eco.common.SystemConfigEnum;
import com.example.eco.core.service.SystemConfigService;
import com.example.eco.model.entity.SystemConfig;
import com.example.eco.model.entity.SystemConfigLog;
import com.example.eco.model.mapper.SystemConfigLogMapper;
import com.example.eco.model.mapper.SystemConfigMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    @Resource
    private SystemConfigMapper systemConfigMapper;

    @Resource
    private SystemConfigLogMapper systemConfigLogMapper;

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

        SystemConfigLog systemConfigLog = new SystemConfigLog();
        systemConfigLog.setName(systemConfig.getName());
        systemConfigLog.setValue(systemConfig.getValue());
        systemConfigLog.setCreateTime(System.currentTimeMillis());
        systemConfigLogMapper.insert(systemConfigLog);

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
        for (SystemConfig systemConfig : systemConfigList) {
            SystemConfigDTO systemConfigDTO = new SystemConfigDTO();
            systemConfigDTO.setName(systemConfig.getName());
            systemConfigDTO.setValue(systemConfig.getValue());
            systemConfigDTOList.add(systemConfigDTO);
        }
        return MultiResponse.of(systemConfigDTOList);
    }

    @Override
    public MultiResponse<SystemConfigLogDTO> list(SystemConfigLogPageQry systemConfigLogPageQry) {

        LambdaQueryWrapper<SystemConfigLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemConfigLog::getName,systemConfigLogPageQry.getName());

        if (Objects.nonNull(systemConfigLogPageQry.getStartTime()) && Objects.nonNull(systemConfigLogPageQry.getEndTime())) {
            queryWrapper.between(SystemConfigLog::getCreateTime,systemConfigLogPageQry.getStartTime(),systemConfigLogPageQry.getEndTime());
        }

        List<SystemConfigLog> systemConfigLogs = systemConfigLogMapper.selectList(queryWrapper);

        List<SystemConfigLogDTO> systemConfigLogDTOList = new ArrayList<>();
        for (SystemConfigLog systemConfigLog : systemConfigLogs){

            SystemConfigLogDTO systemConfigLogDTO = new SystemConfigLogDTO();
            BeanUtils.copyProperties(systemConfigLog,systemConfigLogDTO);

            systemConfigLogDTOList.add(systemConfigLogDTO);

        }
        return MultiResponse.of(systemConfigLogDTOList);
    }

    @Override
    public SingleResponse<SystemConfigDTO> getEcoPrice() {
        LambdaQueryWrapper<SystemConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemConfig::getName, SystemConfigEnum.ECO_PRICE.getCode());

        SystemConfig systemConfig = systemConfigMapper.selectOne(queryWrapper);
        if (Objects.isNull(systemConfig)){
            return SingleResponse.buildFailure("没有设置ECO价格");
        }

        SystemConfigDTO systemConfigDTO = new SystemConfigDTO();
        systemConfigDTO.setName(systemConfig.getName());
        systemConfigDTO.setValue(systemConfig.getValue());

        return SingleResponse.of(systemConfigDTO);
    }
}
