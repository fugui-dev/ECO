package com.example.eco.core.service;

import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.WithdrawRecordCreateCmd;
import com.example.eco.bean.cmd.WithdrawRecordDealWithCmd;

public interface WithdrawRecordService {

    /**
     * 创建提现
     */
    SingleResponse<Void> create(WithdrawRecordCreateCmd withdrawRecordCreateCmd);


    /**
     * 处理提现
     */
    SingleResponse<Void> dealWith(WithdrawRecordDealWithCmd withdrawRecordDealWithCmd);
}
