package com.example.eco.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.eco.model.entity.AccountTransaction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountTransactionMapper extends BaseMapper<AccountTransaction> {
}
