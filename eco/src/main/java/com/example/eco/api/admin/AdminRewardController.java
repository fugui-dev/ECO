package com.example.eco.api.admin;

import com.example.eco.bean.SingleResponse;
import com.example.eco.core.task.RewardScheduled;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/admin/reward")
public class AdminRewardController {

    @Resource
    private RewardScheduled rewardScheduled;

    /**
     * 重跑某天的奖励发放
     * @param dayTime 日期，格式：yyyy-MM-dd
     * @return 执行结果
     */
    @PostMapping("/rerun")
    public SingleResponse<String> rerunReward(@RequestParam String dayTime) {
        log.info("管理员请求重跑{}的奖励发放", dayTime);
        return rewardScheduled.rerunReward(dayTime);
    }

    /**
     * 清除某天的奖励记录
     * @param dayTime 日期，格式：yyyy-MM-dd
     * @return 执行结果
     */
    @PostMapping("/clear")
    public SingleResponse<String> clearReward(@RequestParam String dayTime) {
        log.info("管理员请求清除{}的奖励记录", dayTime);
        return rewardScheduled.clearReward(dayTime);
    }

    /**
     * 重跑某天的奖励发放（先清除再重新发放）
     * @param dayTime 日期，格式：yyyy-MM-dd
     * @return 执行结果
     */
    @PostMapping("/rerun-with-clear")
    public SingleResponse<String> rerunRewardWithClear(@RequestParam String dayTime) {
        log.info("管理员请求重跑{}的奖励发放（先清除再重新发放）", dayTime);
        return rewardScheduled.rerunRewardWithClear(dayTime);
    }

    /**
     * 查询某天的奖励记录统计
     * @param dayTime 日期，格式：yyyy-MM-dd
     * @return 统计结果
     */
    @GetMapping("/statistics")
    public SingleResponse<String> getRewardStatistics(@RequestParam String dayTime) {
        log.info("管理员请求查询{}的奖励记录统计", dayTime);
        return rewardScheduled.getRewardStatistics(dayTime);
    }
}