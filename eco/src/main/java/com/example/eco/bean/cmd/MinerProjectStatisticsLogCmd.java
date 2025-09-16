package com.example.eco.bean.cmd;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MinerProjectStatisticsLogCmd {

    /**
     * 矿机项目ID
     */
    private Integer minerProjectId;

    /**
     * 消耗ESG数量
     */
    private BigDecimal esgNumber;

    /**
     * 金额
     */
    private BigDecimal amount;


    /**
     * 矿机限额
     */
    private String quota;

    /**
     * 日期
     */
    private String dayTime;
}
