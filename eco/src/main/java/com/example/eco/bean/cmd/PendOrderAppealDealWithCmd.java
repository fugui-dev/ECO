package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class PendOrderAppealDealWithCmd {

    /**
     * 申请ID
     */
    private Integer appealId;

    /**
     * 处理状态
     */
    private String status;

    /**
     * 处理理由
     */
    private String reason;
}
