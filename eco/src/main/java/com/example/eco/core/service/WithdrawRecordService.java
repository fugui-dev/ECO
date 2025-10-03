package com.example.eco.core.service;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.WithdrawRecordCreateCmd;
import com.example.eco.bean.cmd.WithdrawRecordDealWithCmd;
import com.example.eco.bean.cmd.WithdrawRecordBatchDealWithCmd;
import com.example.eco.bean.cmd.WithdrawRecordPageQry;
import com.example.eco.bean.cmd.withdrawRecordCancelCmd;
import com.example.eco.bean.dto.WithdrawRecordDTO;
import com.example.eco.bean.dto.WithdrawRecordBatchResultDTO;

public interface WithdrawRecordService {

    /**
     * 创建提现
     */
    SingleResponse<Void> create(WithdrawRecordCreateCmd withdrawRecordCreateCmd);


    /**
     * 处理提现
     */
    SingleResponse<Void> dealWith(WithdrawRecordDealWithCmd withdrawRecordDealWithCmd);


    /**
     * 取消提现
     */
    SingleResponse<Void> cancel(withdrawRecordCancelCmd withdrawRecordCancelCmd);


    /**
     * 分页查询提现记录
     */
    MultiResponse<WithdrawRecordDTO> page(WithdrawRecordPageQry withdrawRecordPageQry);
    
    /**
     * 批量处理提现记录
     */
    SingleResponse<WithdrawRecordBatchResultDTO> batchDealWith(WithdrawRecordBatchDealWithCmd batchDealWithCmd);
}
