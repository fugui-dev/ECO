package com.example.eco.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.eco.model.entity.RewardLevelConfig;
import com.example.eco.model.entity.SystemConfigLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SystemConfigLogMapper extends BaseMapper<SystemConfigLog> {
}
