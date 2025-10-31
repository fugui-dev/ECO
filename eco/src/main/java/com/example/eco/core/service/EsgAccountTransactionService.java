package com.example.eco.core.service;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.cmd.AccountTransactionPageQry;
import com.example.eco.bean.dto.AccountTransactionDTO;
import com.example.eco.bean.dto.EsgAccountTransactionDTO;

public interface EsgAccountTransactionService {

    /**
     * 分页查询账户交易记录
     */
    MultiResponse<EsgAccountTransactionDTO> page(AccountTransactionPageQry accountTransactionPageQry);

}
