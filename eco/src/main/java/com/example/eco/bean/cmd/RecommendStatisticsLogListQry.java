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
    private String dayTime;

    /**
     * 是否按层级查询
     */
    private Boolean isLevel;
}
