package com.example.eco.core.service;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.bean.dto.RecommendComputingPowerDTO;
import com.example.eco.bean.dto.RecommendStatisticsLogDTO;

public interface RecommendStatisticsLogService {

    /**
     * 添加直推人数
     */
    SingleResponse<Void> statistics(DirectRecommendCountCmd directRecommendCountCmd);

    /**
     * 添加算力
     */
    SingleResponse<Void> statistics(TotalComputingPowerCmd totalComputingPowerCmd);


    /**
     * 查询推荐统计日志
     */
    SingleResponse<RecommendStatisticsLogDTO> get(RecommendStatisticsLogQry recommendStatisticsLogQry);

    /**
     * 列表查询推荐统计日志
     */
    MultiResponse<RecommendStatisticsLogDTO> list(RecommendStatisticsLogListQry recommendStatisticsLogListQry);

}
