package com.example.eco.api.admin;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.cmd.RewardStatisticsLogPageQry;
import com.example.eco.bean.dto.RewardStatisticsLogDTO;
import com.example.eco.core.service.RewardStatisticsLogService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/v1/admin/reward/statistics/log")
public class AdminRewardStatisticsLogController {


    @Resource
    private RewardStatisticsLogService rewardStatisticsLogService;

    /**
     * 获取每天奖励统计
     */
    @PostMapping("/page")
    MultiResponse<RewardStatisticsLogDTO> page(@RequestBody RewardStatisticsLogPageQry rewardStatisticsLogPageQry){
        return rewardStatisticsLogService.page(rewardStatisticsLogPageQry);
    }
}
