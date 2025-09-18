package com.example.eco.core.service;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.cmd.RewardStatisticsLogPageQry;
import com.example.eco.bean.dto.RewardStatisticsLogDTO;

public interface RewardStatisticsLogService {

    MultiResponse<RewardStatisticsLogDTO> page(RewardStatisticsLogPageQry rewardStatisticsLogPageQry);
}
