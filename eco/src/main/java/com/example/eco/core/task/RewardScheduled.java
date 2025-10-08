package com.example.eco.core.task;

import com.example.eco.bean.cmd.PurchaseMinerProjectRewardCmd;
import com.example.eco.bean.SingleResponse;
import com.example.eco.common.AccountType;
import com.example.eco.core.cmd.RewardConstructor;
import com.example.eco.core.service.PurchaseMinerProjectService;
import com.example.eco.model.entity.PurchaseMinerProject;
import com.example.eco.model.entity.PurchaseMinerProjectReward;
import com.example.eco.model.entity.RewardStatisticsLog;
import com.example.eco.model.entity.AccountTransaction;
import com.example.eco.model.entity.RecommendStatisticsLog;
import com.example.eco.model.entity.Account;
import com.example.eco.model.entity.SystemConfigLog;
import com.example.eco.model.entity.SystemConfig;
import com.example.eco.model.mapper.PurchaseMinerProjectRewardMapper;
import com.example.eco.model.mapper.PurchaseMinerProjectMapper;
import com.example.eco.model.mapper.RewardStatisticsLogMapper;
import com.example.eco.model.mapper.AccountTransactionMapper;
import com.example.eco.model.mapper.RecommendStatisticsLogMapper;
import com.example.eco.model.mapper.AccountMapper;
import com.example.eco.model.mapper.SystemConfigLogMapper;
import com.example.eco.model.mapper.SystemConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;

@Slf4j
@Component
@Transactional
public class RewardScheduled {

    @Resource
    private RewardConstructor rewardConstructor;
    
    @Resource
    private PurchaseMinerProjectRewardMapper purchaseMinerProjectRewardMapper;
    
    @Resource
    private RewardStatisticsLogMapper rewardStatisticsLogMapper;
    
    @Resource
    private AccountTransactionMapper accountTransactionMapper;
    
    @Resource
    private RecommendStatisticsLogMapper recommendStatisticsLogMapper;
    
    @Resource
    private AccountMapper accountMapper;
    
    @Resource
    private PurchaseMinerProjectMapper purchaseMinerProjectMapper;
    
    @Resource
    private SystemConfigLogMapper systemConfigLogMapper;
    
    @Resource
    private SystemConfigMapper systemConfigMapper;

    @Scheduled(cron = "0 0 0 * * ?")
    @SneakyThrows
    public void reward(){
        log.info("reward 开始执行");

        String dayTime = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        PurchaseMinerProjectRewardCmd purchaseMinerProjectRewardCmd = new PurchaseMinerProjectRewardCmd();
        purchaseMinerProjectRewardCmd.setDayTime(dayTime);

        rewardConstructor.reward(purchaseMinerProjectRewardCmd);

        log.info("reward 执行结束");
    }
    
    /**
     * 手动重跑某天的奖励发放
     * @param dayTime 日期，格式：yyyy-MM-dd
     * @return 执行结果
     */
    public SingleResponse<String> rerunReward(String dayTime) {
        log.info("开始重跑{}的奖励发放", dayTime);
        
        try {
            // 1. 验证日期格式
            if (dayTime == null || dayTime.trim().isEmpty()) {
                return SingleResponse.buildFailure("日期不能为空");
            }
            
            // 2. 检查是否已经发放过奖励
            LambdaQueryWrapper<PurchaseMinerProjectReward> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PurchaseMinerProjectReward::getDayTime, dayTime);
            
            List<PurchaseMinerProjectReward> existingRewards = purchaseMinerProjectRewardMapper.selectList(queryWrapper);
            
            if (!existingRewards.isEmpty()) {
                log.warn("日期{}已经发放过奖励，共{}条记录", dayTime, existingRewards.size());
                return SingleResponse.buildFailure("该日期已发放过奖励，共" + existingRewards.size() + "条记录。如需重新发放，请先调用clearReward方法清除现有记录");
            }
            
            // 3. 执行奖励发放
            PurchaseMinerProjectRewardCmd purchaseMinerProjectRewardCmd = new PurchaseMinerProjectRewardCmd();
            purchaseMinerProjectRewardCmd.setDayTime(dayTime);
            
            SingleResponse<?> result = rewardConstructor.reward(purchaseMinerProjectRewardCmd);
            
            if (result.isSuccess()) {
                log.info("重跑{}的奖励发放成功", dayTime);
                return SingleResponse.of("重跑奖励发放成功");
            } else {
                log.error("重跑{}的奖励发放失败: {}", dayTime, result.getErrMessage());
                return SingleResponse.buildFailure("重跑奖励发放失败: " + result.getErrMessage());
            }
            
        } catch (Exception e) {
            log.error("重跑{}的奖励发放异常", dayTime, e);
            return SingleResponse.buildFailure("重跑奖励发放异常: " + e.getMessage());
        }
    }
    
    /**
     * 清除某天的奖励记录
     * @param dayTime 日期，格式：yyyy-MM-dd
     * @return 执行结果
     */
    public SingleResponse<String> clearReward(String dayTime) {
        log.info("开始清除{}的奖励记录", dayTime);
        
        try {
            // 1. 验证日期格式
            if (dayTime == null || dayTime.trim().isEmpty()) {
                return SingleResponse.buildFailure("日期不能为空");
            }
            
            // 2. 查询现有奖励记录
            LambdaQueryWrapper<PurchaseMinerProjectReward> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PurchaseMinerProjectReward::getDayTime, dayTime);
            
            List<PurchaseMinerProjectReward> existingRewards = purchaseMinerProjectRewardMapper.selectList(queryWrapper);
            
            if (existingRewards.isEmpty()) {
                log.info("日期{}没有奖励记录", dayTime);
                return SingleResponse.of("该日期没有奖励记录");
            }
            
            // 3. 获取当天的价格
            BigDecimal price = getPriceForDay(dayTime);
            log.info("使用价格: {} 进行奖励清除", price);

            if (price.compareTo(BigDecimal.ZERO) == 0){
                return SingleResponse.buildFailure("没找到当日的ECO价格");
            }
            
            // 4. 根据奖励记录逐条处理
            int accountCount = 0;
            int transactionCount = 0;
            int minerCount = 0;
            
            for (PurchaseMinerProjectReward rewardRecord : existingRewards) {
                String walletAddress = rewardRecord.getWalletAddress();
                String order = rewardRecord.getOrder();
                BigDecimal rewardAmount = new BigDecimal(rewardRecord.getReward());
                String rewardType = rewardRecord.getType();
                Integer purchaseMinerProjectId = rewardRecord.getPurchaseMinerProjectId();
                
                // 3.1 更新账户字段
                LambdaQueryWrapper<Account> accountQuery = new LambdaQueryWrapper<>();
                accountQuery.eq(Account::getWalletAddress, walletAddress);
                accountQuery.eq(Account::getType, AccountType.ECO.getCode());
                Account account = accountMapper.selectOne(accountQuery);
                
                if (account != null) {
                    // 减少number字段（可用积分余额）
                    BigDecimal currentNumber = new BigDecimal(account.getNumber());
                    BigDecimal newNumber = currentNumber.subtract(rewardAmount);
                    account.setNumber(newNumber.toString());
                    
                    // 根据奖励类型减少相应的累计奖励字段
                    if ("STATIC".equals(rewardType)) {
                        BigDecimal currentStaticReward = new BigDecimal(account.getStaticReward());
                        BigDecimal newStaticReward = currentStaticReward.subtract(rewardAmount);
                        account.setStaticReward(newStaticReward.toString());
                    } else if ("DYNAMIC".equals(rewardType)) {
                        BigDecimal currentDynamicReward = new BigDecimal(account.getDynamicReward());
                        BigDecimal newDynamicReward = currentDynamicReward.subtract(rewardAmount);
                        account.setDynamicReward(newDynamicReward.toString());
                    }
                    
                    accountMapper.updateById(account);
                    accountCount++;
                }
                
                // 3.2 更新矿机表的已发放数量和价格
                if (purchaseMinerProjectId != null) {
                    LambdaQueryWrapper<PurchaseMinerProject> minerQuery = new LambdaQueryWrapper<>();
                    minerQuery.eq(PurchaseMinerProject::getId, purchaseMinerProjectId);
                    PurchaseMinerProject minerProject = purchaseMinerProjectMapper.selectOne(minerQuery);
                    
                    if (minerProject != null && new BigDecimal(minerProject.getReward()).compareTo(BigDecimal.ZERO) > 0) {

//                        log.info("减少已发放奖励数量 :{}",minerProject.getReward());
//                        // 减少已发放奖励数量
//                        BigDecimal currentReward = new BigDecimal(minerProject.getReward());
//                        BigDecimal newReward = currentReward.subtract(rewardAmount);
//                        minerProject.setReward(newReward.toString());
//
//                        // 减少已发放奖励的总价值（使用正确的价格比例）
//                        BigDecimal currentRewardPrice = new BigDecimal(minerProject.getRewardPrice());
//                        BigDecimal rewardPriceAmount = rewardAmount.multiply(price);
//                        BigDecimal newRewardPrice = currentRewardPrice.subtract(rewardPriceAmount);
//                        minerProject.setRewardPrice(newRewardPrice.toString());
//
//                        purchaseMinerProjectMapper.updateById(minerProject);
//                        minerCount++;
                    }
                }
                
                // 3.3 删除对应的账户交易记录（根据订单号）
                LambdaQueryWrapper<AccountTransaction> transactionQuery = new LambdaQueryWrapper<>();
                transactionQuery.eq(AccountTransaction::getOrder, order);

                int deletedTransactionCount = accountTransactionMapper.delete(transactionQuery);
                transactionCount += deletedTransactionCount;
            }
            
            // 4. 删除奖励记录
            int deletedCount = purchaseMinerProjectRewardMapper.delete(queryWrapper);
            
            // 5. 删除奖励统计记录
            LambdaQueryWrapper<RewardStatisticsLog> statisticsQueryWrapper = new LambdaQueryWrapper<>();
            statisticsQueryWrapper.eq(RewardStatisticsLog::getDayTime, dayTime);
            int statisticsCount = rewardStatisticsLogMapper.delete(statisticsQueryWrapper);
            
            int totalCount = deletedCount + statisticsCount + transactionCount;
            
            log.info("成功清除{}的奖励数据，共删除{}条记录（奖励记录:{}，统计记录:{}，交易记录:{}），重置账户奖励字段:{}，更新矿机表:{}，使用价格:{}", 
                dayTime, totalCount, deletedCount, statisticsCount, transactionCount, accountCount, minerCount, price);
            return SingleResponse.of("成功清除" + totalCount + "条记录，重置" + accountCount + "个账户的奖励字段，更新" + minerCount + "个矿机的已发放数据，使用价格:" + price);
            
        } catch (Exception e) {
            log.error("清除{}的奖励记录异常", dayTime, e);
            return SingleResponse.buildFailure("清除奖励记录异常: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定日期的价格
     * 优先从SystemConfigLog获取当天的价格，如果没有则获取这天之前的最后一次记录，如果还没有则从SystemConfig获取
     * @param dayTime 日期，格式：yyyy-MM-dd
     * @return 价格
     */
    private BigDecimal getPriceForDay(String dayTime) {
        try {
            // 将日期转换为时间戳范围
            Long endTime = LocalDate.parse(dayTime, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    .plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

            // 2. 获取这天之前的最后一次记录
            LambdaQueryWrapper<SystemConfigLog> beforeQuery = new LambdaQueryWrapper<>();
            beforeQuery.eq(SystemConfigLog::getName, "ECO_PRICE");
            beforeQuery.lt(SystemConfigLog::getCreateTime, endTime);
            beforeQuery.orderByDesc(SystemConfigLog::getCreateTime);
            beforeQuery.last("LIMIT 1");
            SystemConfigLog beforeConfig = systemConfigLogMapper.selectOne(beforeQuery);
            
            if (beforeConfig != null && beforeConfig.getValue() != null && !beforeConfig.getValue().trim().isEmpty()) {
                log.info("获取到{}之前的最后一次价格: {}", dayTime, beforeConfig.getValue());
                return new BigDecimal(beforeConfig.getValue());
            }
            
            // 3. 从SystemConfig获取默认价格
            LambdaQueryWrapper<SystemConfig> configQuery = new LambdaQueryWrapper<>();
            configQuery.eq(SystemConfig::getName, "ECO_PRICE");
            SystemConfig config = systemConfigMapper.selectOne(configQuery);
            
            if (config != null && config.getValue() != null && !config.getValue().trim().isEmpty()) {
                log.info("获取到系统默认价格: {}", config.getValue());
                return new BigDecimal(config.getValue());
            }
            
            log.warn("未找到价格配置，使用默认价格1");
            return BigDecimal.ZERO;
            
        } catch (Exception e) {
            log.error("获取{}的价格异常", dayTime, e);
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * 重跑某天的奖励发放（先清除再重新发放）
     * @param dayTime 日期，格式：yyyy-MM-dd
     * @return 执行结果
     */
    public SingleResponse<String> rerunRewardWithClear(String dayTime) {
        log.info("开始重跑{}的奖励发放（先清除再重新发放）", dayTime);
        
        try {
            // 1. 先清除现有记录
            SingleResponse<String> clearResult = clearReward(dayTime);
            if (!clearResult.isSuccess()) {
                return clearResult;
            }
            
            // 2. 重新发放奖励
            SingleResponse<String> rewardResult = rerunReward(dayTime);
            if (!rewardResult.isSuccess()) {
                return rewardResult;
            }
            
            log.info("重跑{}的奖励发放完成（先清除再重新发放）", dayTime);
            return SingleResponse.of("重跑奖励发放完成");
            
        } catch (Exception e) {
            log.error("重跑{}的奖励发放异常（先清除再重新发放）", dayTime, e);
            return SingleResponse.buildFailure("重跑奖励发放异常: " + e.getMessage());
        }
    }
    
    /**
     * 查询某天的奖励记录统计
     * @param dayTime 日期，格式：yyyy-MM-dd
     * @return 统计结果
     */
    public SingleResponse<String> getRewardStatistics(String dayTime) {
        log.info("查询{}的奖励记录统计", dayTime);
        
        try {
            // 1. 验证日期格式
            if (dayTime == null || dayTime.trim().isEmpty()) {
                return SingleResponse.buildFailure("日期不能为空");
            }
            
            // 2. 查询奖励记录
            LambdaQueryWrapper<PurchaseMinerProjectReward> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PurchaseMinerProjectReward::getDayTime, dayTime);
            
            List<PurchaseMinerProjectReward> rewards = purchaseMinerProjectRewardMapper.selectList(queryWrapper);
            
            if (rewards.isEmpty()) {
                return SingleResponse.of("该日期没有奖励记录");
            }
            
            // 3. 统计信息
            int totalCount = rewards.size();
            int staticCount = (int) rewards.stream().filter(r -> "STATIC".equals(r.getType())).count();
            int dynamicCount = (int) rewards.stream().filter(r -> "DYNAMIC".equals(r.getType())).count();
            
            String statistics = String.format("日期: %s, 总记录数: %d, 静态奖励: %d, 动态奖励: %d", 
                dayTime, totalCount, staticCount, dynamicCount);
            
            log.info("{}的奖励记录统计: {}", dayTime, statistics);
            return SingleResponse.of(statistics);
            
        } catch (Exception e) {
            log.error("查询{}的奖励记录统计异常", dayTime, e);
            return SingleResponse.buildFailure("查询奖励记录统计异常: " + e.getMessage());
        }
    }
}
