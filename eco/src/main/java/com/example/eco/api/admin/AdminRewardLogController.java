package com.example.eco.api.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eco.bean.SingleResponse;
import com.example.eco.model.entity.RewardLog;
import com.example.eco.model.mapper.RewardLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/reward-log")
public class AdminRewardLogController {

    @Resource
    private RewardLogMapper rewardLogMapper;

    /**
     * 查询指定日期的奖励日志
     * @param dayTime 日期，格式：yyyy-MM-dd
     * @return 奖励日志列表
     */
    @GetMapping("/list")
    public SingleResponse<List<RewardLog>> getRewardLogs(@RequestParam String dayTime) {
        log.info("查询{}的奖励日志", dayTime);
        
        try {
            LambdaQueryWrapper<RewardLog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(RewardLog::getDayTime, dayTime);
            queryWrapper.orderByDesc(RewardLog::getCreateTime);
            
            List<RewardLog> rewardLogs = rewardLogMapper.selectList(queryWrapper);
            
            log.info("查询到{}条奖励日志", rewardLogs.size());
            return SingleResponse.of(rewardLogs);
            
        } catch (Exception e) {
            log.error("查询奖励日志异常", e);
            return SingleResponse.buildFailure("查询奖励日志异常: " + e.getMessage());
        }
    }

    /**
     * 查询指定用户的奖励日志
     * @param walletAddress 钱包地址
     * @param dayTime 日期，格式：yyyy-MM-dd
     * @return 奖励日志列表
     */
    @GetMapping("/user")
    public SingleResponse<List<RewardLog>> getUserRewardLogs(@RequestParam String walletAddress, 
                                                           @RequestParam String dayTime) {
        log.info("查询用户{}在{}的奖励日志", walletAddress, dayTime);
        
        try {
            LambdaQueryWrapper<RewardLog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(RewardLog::getWalletAddress, walletAddress);
            queryWrapper.eq(RewardLog::getDayTime, dayTime);
            queryWrapper.orderByDesc(RewardLog::getCreateTime);
            
            List<RewardLog> rewardLogs = rewardLogMapper.selectList(queryWrapper);
            
            log.info("查询到用户{}的{}条奖励日志", walletAddress, rewardLogs.size());
            return SingleResponse.of(rewardLogs);
            
        } catch (Exception e) {
            log.error("查询用户奖励日志异常", e);
            return SingleResponse.buildFailure("查询用户奖励日志异常: " + e.getMessage());
        }
    }

    /**
     * 查询奖励统计信息
     * @param dayTime 日期，格式：yyyy-MM-dd
     * @return 统计信息
     */
    @GetMapping("/statistics")
    public SingleResponse<String> getRewardLogStatistics(@RequestParam String dayTime) {
        log.info("查询{}的奖励日志统计", dayTime);
        
        try {
            LambdaQueryWrapper<RewardLog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(RewardLog::getDayTime, dayTime);
            
            List<RewardLog> rewardLogs = rewardLogMapper.selectList(queryWrapper);
            
            if (rewardLogs.isEmpty()) {
                return SingleResponse.of("该日期没有奖励日志记录");
            }
            
            // 统计信息
            int totalCount = rewardLogs.size();
            int staticCount = (int) rewardLogs.stream().filter(log -> "STATIC".equals(log.getRewardType())).count();
            int dynamicCount = (int) rewardLogs.stream().filter(log -> "DYNAMIC".equals(log.getRewardType())).count();
            int discardedCount = (int) rewardLogs.stream().filter(log -> 
                log.getDiscardedReward() != null && !"0".equals(log.getDiscardedReward())).count();
            int noRewardCount = (int) rewardLogs.stream().filter(log -> 
                log.getActualReward() != null && "0".equals(log.getActualReward())).count();
            
            String statistics = String.format("日期: %s, 总记录数: %d, 静态奖励: %d, 动态奖励: %d, 有舍去奖励: %d, 无奖励用户: %d", 
                dayTime, totalCount, staticCount, dynamicCount, discardedCount, noRewardCount);
            
            log.info("{}的奖励日志统计: {}", dayTime, statistics);
            return SingleResponse.of(statistics);
            
        } catch (Exception e) {
            log.error("查询奖励日志统计异常", e);
            return SingleResponse.buildFailure("查询奖励日志统计异常: " + e.getMessage());
        }
    }

    /**
     * 查询没有获得奖励的用户
     * @param dayTime 日期，格式：yyyy-MM-dd
     * @return 没有获得奖励的用户列表
     */
    @GetMapping("/no-reward")
    public SingleResponse<List<RewardLog>> getNoRewardUsers(@RequestParam String dayTime) {
        log.info("查询{}没有获得奖励的用户", dayTime);
        
        try {
            LambdaQueryWrapper<RewardLog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(RewardLog::getDayTime, dayTime);
            queryWrapper.eq(RewardLog::getActualReward, "0");
            queryWrapper.orderByDesc(RewardLog::getCreateTime);
            
            List<RewardLog> noRewardLogs = rewardLogMapper.selectList(queryWrapper);
            
            log.info("查询到{}个没有获得奖励的用户", noRewardLogs.size());
            return SingleResponse.of(noRewardLogs);
            
        } catch (Exception e) {
            log.error("查询没有获得奖励的用户异常", e);
            return SingleResponse.buildFailure("查询没有获得奖励的用户异常: " + e.getMessage());
        }
    }

    /**
     * 查询有舍去奖励的用户
     * @param dayTime 日期，格式：yyyy-MM-dd
     * @return 有舍去奖励的用户列表
     */
    @GetMapping("/discarded")
    public SingleResponse<List<RewardLog>> getDiscardedRewardUsers(@RequestParam String dayTime) {
        log.info("查询{}有舍去奖励的用户", dayTime);
        
        try {
            LambdaQueryWrapper<RewardLog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(RewardLog::getDayTime, dayTime);
            queryWrapper.ne(RewardLog::getDiscardedReward, "0");
            queryWrapper.isNotNull(RewardLog::getDiscardedReward);
            queryWrapper.orderByDesc(RewardLog::getCreateTime);
            
            List<RewardLog> discardedLogs = rewardLogMapper.selectList(queryWrapper);
            
            log.info("查询到{}个有舍去奖励的用户", discardedLogs.size());
            return SingleResponse.of(discardedLogs);
            
        } catch (Exception e) {
            log.error("查询有舍去奖励的用户异常", e);
            return SingleResponse.buildFailure("查询有舍去奖励的用户异常: " + e.getMessage());
        }
    }
}