package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class WithdrawRecordDealWithCmd {

    private Integer id;
    /**
     * 提现状态
     */
    private String status;

    /**
     * 审核拒绝原因
     */
    /**
     * 审核拒绝原因
     */
    private String reason;
}
