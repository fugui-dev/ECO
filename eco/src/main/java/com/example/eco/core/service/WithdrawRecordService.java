package com.example.eco.core.service;

import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.WithdrawRecordCreateCmd;

public interface WithdrawRecordService {

    /**
     * 创建提现
     */
    SingleResponse<Void> create(WithdrawRecordCreateCmd withdrawRecordCreateCmd);
}
