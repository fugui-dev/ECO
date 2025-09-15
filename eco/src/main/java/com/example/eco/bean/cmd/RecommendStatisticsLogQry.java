package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class RecommendStatisticsLogQry {

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 日期
     */
    private String dayTime;

    /**
     * 是否查询全部
     */
    private Boolean isLevel;
}
