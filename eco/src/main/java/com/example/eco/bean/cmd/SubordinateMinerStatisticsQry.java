package com.example.eco.bean.cmd;

import lombok.Data;

/**
 * 伞下矿机统计查询命令
 */
@Data
public class SubordinateMinerStatisticsQry {

    /**
     * 钱包地址（查询该地址的伞下）
     */
    private String walletAddress;

    /**
     * 年份
     */
    private Integer year;

    /**
     * 月份
     */
    private Integer month;
}
