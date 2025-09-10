package com.example.eco.core.service;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.AccountTransactionCreateCmd;
import com.example.eco.bean.cmd.AccountTransactionPageQry;
import com.example.eco.bean.dto.AccountTransactionDTO;

public interface AccountTransactionService {

    /**
     * 分页查询账户交易记录
     */
    MultiResponse<AccountTransactionDTO> page(AccountTransactionPageQry accountTransactionPageQry);

}
