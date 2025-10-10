package com.example.eco.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.eco.model.entity.TokenTransferLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 代币转账记录Mapper
 */
@Mapper
public interface TokenTransferLogMapper extends BaseMapper<TokenTransferLog> {
}