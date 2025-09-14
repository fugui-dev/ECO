package com.example.eco.bean.cmd;

import com.example.eco.bean.PageQuery;
import lombok.Data;

@Data
public class RecommendStatisticsLogListQry {

    /**
     * 被推荐人钱包地址
     */
    private String walletAddress;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;
}
