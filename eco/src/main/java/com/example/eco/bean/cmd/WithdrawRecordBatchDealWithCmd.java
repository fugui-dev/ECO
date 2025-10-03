package com.example.eco.bean.cmd;

import lombok.Data;
import java.util.List;

/**
 * 批量处理提现记录命令
 */
@Data
public class WithdrawRecordBatchDealWithCmd {
    
    /**
     * 提现记录ID列表
     */
    private List<Integer> ids;
    
    /**
     * 提现状态
     */
    private String status;
    
    /**
     * 审核拒绝原因（拒绝时必填）
     */
    private String reason;
}