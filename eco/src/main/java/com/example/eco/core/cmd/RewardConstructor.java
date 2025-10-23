package com.example.eco.core.cmd;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.bean.dto.RecommendDTO;
import com.example.eco.bean.dto.RecommendStatisticsLogDTO;
import com.example.eco.bean.dto.ComputingPowerDTO;
import com.example.eco.core.service.ComputingPowerService;
import com.example.eco.core.service.impl.ComputingPowerServiceImplV2;
import com.example.eco.common.*;
import com.example.eco.core.service.AccountService;
import com.example.eco.core.service.RecommendService;
import com.example.eco.core.service.RecommendStatisticsLogService;
import com.example.eco.model.entity.*;
import com.example.eco.model.mapper.*;
import com.example.eco.util.ComputingPowerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Objects;

@Slf4j
@Component
public class RewardConstructor {

    @Resource
    private PurchaseMinerProjectMapper purchaseMinerProjectMapper;
    @Resource
    private RewardStatisticsLogMapper rewardStatisticsLogMapper;
    @Resource
    private AccountService accountService;
    @Resource
    private SystemConfigMapper systemConfigMapper;
    @Resource
    private MinerConfigMapper minerConfigMapper;
    @Resource
    private RecommendMapper recommendMapper;
    @Resource
    private PurchaseMinerProjectRewardMapper purchaseMinerProjectRewardMapper;
    @Resource
    private RecommendStatisticsLogService recommendStatisticsLogService;
    @Resource(name = "computingPowerService")
    private ComputingPowerService computingPowerService;
    @Resource
    private ComputingPowerServiceImplV2 computingPowerServiceV2;
    @Resource
    private RewardLogMapper rewardLogMapper;
    @Resource
    private RecommendService recommendService;
    @Resource
    private MinerDailyRewardMapper minerDailyRewardMapper;
    @Resource
    private AccountMapper accountMapper;
    @Resource
    private RewardServiceLogMapper rewardServiceLogMapper;
    @Resource
    private SystemConfigLogMapper systemConfigLogMapper;

    /**
     * 发放算力奖励
     */
    public SingleResponse<PurchaseMinerProjectReward> reward(PurchaseMinerProjectRewardCmd purchaseMinerProjectRewardCmd) {
        log.info("=== 开始发放奖励，日期: {} ===", purchaseMinerProjectRewardCmd.getDayTime());

        Long endTime = LocalDate.parse(purchaseMinerProjectRewardCmd.getDayTime()).plusDays(1).atStartOfDay()
                .atZone(ZoneId.systemDefault())  // 明确使用系统时区
                .toInstant()
                .toEpochMilli();

        LambdaQueryWrapper<PurchaseMinerProject> purchaseMinerProjectLambdaQueryWrapper = new LambdaQueryWrapper<>();
        purchaseMinerProjectLambdaQueryWrapper.eq(PurchaseMinerProject::getStatus,PurchaseMinerProjectStatus.SUCCESS.getCode());
        // 只查询指定日期及之前创建的矿机
        purchaseMinerProjectLambdaQueryWrapper.le(PurchaseMinerProject::getCreateTime, endTime);
        List<PurchaseMinerProject> purchaseMinerProjectList = purchaseMinerProjectMapper.selectList(purchaseMinerProjectLambdaQueryWrapper);

        // 使用新的算力计算服务获取总算力
        BigDecimal totalComputingPower = purchaseMinerProjectList.stream()
                .map(PurchaseMinerProject::getActualComputingPower)
                .map(BigDecimal::new)
                .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

        log.info("【总算力统计】总算力: {}", totalComputingPower);

        if (totalComputingPower.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("【总算力检查】总算力为0，无需发放奖励");
            return SingleResponse.buildSuccess();
        }

        // 获取矿机算力要求配置
        SingleResponse<BigDecimal> minerRequirementResponse = getMinerRequirement(totalComputingPower);
        if (!minerRequirementResponse.isSuccess()) {
            log.error("【矿机要求检查】获取矿机算力要求失败: {}", minerRequirementResponse.getErrMessage());
            return SingleResponse.buildFailure(minerRequirementResponse.getErrMessage());
        }

        BigDecimal minerRequirement = minerRequirementResponse.getData();
        log.info("【矿机要求检查】矿机算力要求: {}", minerRequirement);

        // 获取每天总奖励数
        SingleResponse<BigDecimal> totalRewardResponse = getTotalReward(totalComputingPower, minerRequirement);
        if (!totalRewardResponse.isSuccess()) {
            log.error("【总奖励计算】获取每天总奖励数失败: {}", totalRewardResponse.getErrMessage());
            return SingleResponse.buildFailure(totalRewardResponse.getErrMessage());
        }
        BigDecimal totalReward = totalRewardResponse.getData();

        log.info("【奖励发放总览】总算力: {}, 矿机算力要求: {}, 每天总奖励数: {}", totalComputingPower, minerRequirement, totalReward);
        if (totalReward.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("【总奖励检查】每天总奖励数为0，无需发放奖励");
            return SingleResponse.buildSuccess();
        }

        LambdaQueryWrapper<SystemConfig> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(SystemConfig::getName,SystemConfigEnum.ECO_PRICE.getCode());

        SystemConfig ecoPrice = systemConfigMapper.selectOne(lambdaQueryWrapper);

        if (Objects.isNull(ecoPrice)){
            log.error("【价格配置检查】未设置ECO价格，无法发放奖励");
            return SingleResponse.buildSuccess();
        }
        log.info("【价格配置检查】ECO价格: {}", ecoPrice.getValue());

        List<PurchaseMinerProjectReward> rewardList = new ArrayList<>();

        // 计算静态奖励
        log.info("【静态奖励发放】开始发放静态奖励");
        staticReward(totalComputingPower, totalReward, purchaseMinerProjectRewardCmd.getDayTime(), rewardList,ecoPrice);
        log.info("【静态奖励发放】静态奖励发放完成，当前奖励记录数: {}", rewardList.size());

        // 计算动态奖励 - 需要获取所有用户的算力信息
        log.info("【动态奖励发放】开始获取所有用户算力信息");
        List<ComputingPowerDTO> allUsersComputingPower = getAllUsersComputingPower(purchaseMinerProjectRewardCmd.getDayTime());
        log.info("【动态奖励发放】获取到{}个用户的算力信息", allUsersComputingPower.size());

        log.info("【动态奖励发放】开始发放动态奖励");
        dynamicReward(purchaseMinerProjectRewardCmd.getDayTime(), totalReward, allUsersComputingPower, rewardList,ecoPrice);
        log.info("【动态奖励发放】动态奖励发放完成，总奖励记录数: {}", rewardList.size());

        // 统计最终发放情况
        BigDecimal totalActualReward = rewardList.stream()
                .map(PurchaseMinerProjectReward::getReward)
                .map(BigDecimal::new)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
        
        BigDecimal totalDiscardedReward = totalReward.subtract(totalActualReward);
        log.info("【奖励发放总结】计划发放: {}, 实际发放: {}, 舍去奖励: {}, 舍去比例: {}%", 
                totalReward, totalActualReward, totalDiscardedReward, 
                totalReward.compareTo(BigDecimal.ZERO) > 0 ? 
                    totalDiscardedReward.divide(totalReward, 8, RoundingMode.DOWN).multiply(new BigDecimal(100)) : BigDecimal.ZERO);

        // 添加统计数据
        addRewardStatisticsLog(purchaseMinerProjectRewardCmd.getDayTime(), rewardList);

        // 添加扣除服务费
        addRewardServiceLog(purchaseMinerProjectRewardCmd.getDayTime(), rewardList);

        addHistoryPrice(ecoPrice);

        log.info("=== 奖励发放完成，日期: {} ===", purchaseMinerProjectRewardCmd.getDayTime());
        return SingleResponse.buildSuccess();
    }

    /**
     * 检查用户是否有挖矿资格
     */
    public SingleResponse<Recommend> checkRecommend(String walletAddress) {
        log.debug("【挖矿资格检查】开始检查用户{}的挖矿资格", walletAddress);

        //检查是否有矿机
        LambdaQueryWrapper<PurchaseMinerProject> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PurchaseMinerProject::getWalletAddress,walletAddress);
        queryWrapper.eq(PurchaseMinerProject::getStatus,PurchaseMinerProjectStatus.SUCCESS.getCode());

        Long count = purchaseMinerProjectMapper.selectCount(queryWrapper);
        log.debug("【挖矿资格检查】用户{}的可用矿机数量: {}", walletAddress, count);
        
        if (count == 0){
            log.warn("【挖矿资格检查】用户{}没有可用的矿机，跳过奖励发放", walletAddress);
            return SingleResponse.buildFailure("没有可用的矿机");
        }

        RecommendQry recommendQry = new RecommendQry();
        recommendQry.setWalletAddress(walletAddress);

        RecommendDTO recommendDTO = recommendService.get(recommendQry).getData();

        Recommend recommend = new Recommend();
        BeanUtils.copyProperties(recommendDTO,recommend);

        log.debug("【挖矿资格检查】用户{}挖矿资格检查通过，有{}台矿机", walletAddress, count);
        return SingleResponse.of(recommend);
    }


    /**
     * 获取矿机算力要求
     */
    public SingleResponse<BigDecimal> getMinerRequirement(BigDecimal totalComputingPower) {

        LambdaQueryWrapper<MinerConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MinerConfig::getName, MinerConfigEnum.MINER_REQUIREMENT.getCode());

        MinerConfig minerConfig = minerConfigMapper.selectOne(queryWrapper);

        if (minerConfig == null || minerConfig.getValue() == null) {
            return SingleResponse.buildFailure("矿机算力要求配置错误");
        }

        BigDecimal minerRequirement = new BigDecimal(minerConfig.getValue());
        if (minerRequirement.compareTo(BigDecimal.ZERO) <= 0) {
            return SingleResponse.buildFailure("矿机算力要求配置错误");
        }
        if (totalComputingPower.compareTo(minerRequirement) < 0) {
            log.info("总算力{}未达到最小算力要求{}，无法发放奖励", totalComputingPower, minerRequirement);
            return SingleResponse.buildFailure("总算力未达到最小算力要求");
        }
        return SingleResponse.of(minerRequirement);
    }

    /**
     * 获取每天总奖励
     */
    public SingleResponse<BigDecimal> getTotalReward(BigDecimal totalComputingPower, BigDecimal minerRequirement) {

        // 获取每天总奖励数
        LambdaQueryWrapper<SystemConfig> totalRewardQueryWrapper = new LambdaQueryWrapper<>();
        totalRewardQueryWrapper.eq(SystemConfig::getName, SystemConfigEnum.DAILY_TOTAL_REWARD.getCode());

        SystemConfig totalRewardSystemConfig = systemConfigMapper.selectOne(totalRewardQueryWrapper);
        if (totalRewardSystemConfig == null || totalRewardSystemConfig.getValue() == null) {
            return SingleResponse.buildFailure("每天挖矿总数系统配置错误");
        }

        BigDecimal totalReward = new BigDecimal(totalRewardSystemConfig.getValue());
        if (totalReward.compareTo(BigDecimal.ZERO) <= 0) {
            return SingleResponse.buildFailure("每天挖矿总数系统配置错误");
        }

        // 获取每天总奖励数
        LambdaQueryWrapper<SystemConfig> totalRewardLimitQueryWrapper = new LambdaQueryWrapper<>();
        totalRewardLimitQueryWrapper.eq(SystemConfig::getName, SystemConfigEnum.DAILY_TOTAL_REWARD_LIMIT.getCode());

        SystemConfig totalRewardLimitSystemConfig = systemConfigMapper.selectOne(totalRewardLimitQueryWrapper);
        if (totalRewardLimitSystemConfig == null || totalRewardLimitSystemConfig.getValue() == null) {
            return SingleResponse.buildFailure("每天挖矿总数数量上限配置错误");
        }

        BigDecimal totalLimitReward = new BigDecimal(totalRewardLimitSystemConfig.getValue());
        if (totalLimitReward.compareTo(BigDecimal.ZERO) <= 0) {
            return SingleResponse.buildFailure("每天挖矿总数数量上限配置错误");
        }

        String dayTime = LocalDate.now().minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        LambdaQueryWrapper<RewardStatisticsLog> rewardStatisticsLogLambdaQueryWrapper = new LambdaQueryWrapper<>();
        rewardStatisticsLogLambdaQueryWrapper.eq(RewardStatisticsLog::getDayTime, dayTime);

        RewardStatisticsLog rewardStatisticsLog = rewardStatisticsLogMapper.selectOne(rewardStatisticsLogLambdaQueryWrapper);

        if (Objects.nonNull(rewardStatisticsLog)) {

            BigDecimal beforeTotalReward = new BigDecimal(rewardStatisticsLog.getTotalReward());

            if (beforeTotalReward.compareTo(totalLimitReward) >= 0) {

                // 获取每天总奖励数
                LambdaQueryWrapper<SystemConfig> rewardReduceRateQueryWrapper = new LambdaQueryWrapper<>();
                rewardReduceRateQueryWrapper.eq(SystemConfig::getName, SystemConfigEnum.DAILY_TOTAL_REWARD_REDUCE_RATE.getCode());

                SystemConfig rewardReduceRateSystemConfig = systemConfigMapper.selectOne(rewardReduceRateQueryWrapper);
                if (rewardReduceRateSystemConfig == null || rewardReduceRateSystemConfig.getValue() == null) {
                    return SingleResponse.buildFailure("每天挖矿总数数量降低到比例");
                }

                BigDecimal rewardReduceRate = new BigDecimal(rewardReduceRateSystemConfig.getValue());

                totalReward = totalReward.multiply(rewardReduceRate);

            }
        }

        LambdaQueryWrapper<MinerConfig> minerAddNumberRequirementQueryWrapper = new LambdaQueryWrapper<>();
        minerAddNumberRequirementQueryWrapper.eq(MinerConfig::getName, MinerConfigEnum.MINER_ADD_NUMBER_REQUIREMENT.getCode());

        MinerConfig minerAddNumberRequirementConfig = minerConfigMapper.selectOne(minerAddNumberRequirementQueryWrapper);
        if (minerAddNumberRequirementConfig == null || minerAddNumberRequirementConfig.getValue() == null) {
            return SingleResponse.buildFailure("新增矿机挖矿数量要求配置错误");
        }
        BigDecimal minerAddNumberRequirement = new BigDecimal(minerAddNumberRequirementConfig.getValue());
        if (minerAddNumberRequirement.compareTo(BigDecimal.ZERO) <= 0) {
            return SingleResponse.buildFailure("新增矿机挖矿数量要求配置错误");
        }

        // 查询用户所有有效矿机
        LambdaQueryWrapper<PurchaseMinerProject> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PurchaseMinerProject::getStatus, PurchaseMinerProjectStatus.SUCCESS.getCode());

        BigDecimal computingPower  = purchaseMinerProjectMapper.selectList(queryWrapper)
                .stream()
                .map(PurchaseMinerProject::getComputingPower)
                .map(BigDecimal::new)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);


        // 计算出多余的算力
        BigDecimal moreComputingPower = computingPower.subtract(minerRequirement);

        // 计算出新增矿机数量
        if (moreComputingPower.compareTo(minerAddNumberRequirement) < 0) {
            log.info("新增算力{}未达到新增矿机挖矿数量要求{}，不增加挖矿数量", moreComputingPower, minerAddNumberRequirement);
        } else {
            // 新增挖矿数量的倍数 = 新增算力 / 新增矿机挖矿数量要求 （取余）
            BigDecimal times = moreComputingPower.divide(minerAddNumberRequirement, 0, RoundingMode.DOWN);

            LambdaQueryWrapper<MinerConfig> minerAddNumberQueryWrapper = new LambdaQueryWrapper<>();
            minerAddNumberQueryWrapper.eq(MinerConfig::getName, MinerConfigEnum.MINER_ADD_NUMBER.getCode());

            MinerConfig minerAddNumberConfig = minerConfigMapper.selectOne(minerAddNumberQueryWrapper);
            if (minerAddNumberConfig == null || minerAddNumberConfig.getValue() == null) {
                return SingleResponse.buildFailure("新增矿机挖矿数量配置错误");
            }
            BigDecimal minerAddNumber = new BigDecimal(minerAddNumberConfig.getValue());
            if (minerAddNumber.compareTo(BigDecimal.ZERO) <= 0) {
                return SingleResponse.buildFailure("新增矿机挖矿数量配置错误");
            }

            BigDecimal totalAddNumber = minerAddNumber.multiply(times);
            log.info("新增算力{}达到新增矿机挖矿数量要求{}，增加挖矿数量{}", moreComputingPower, minerAddNumberRequirement, totalAddNumber);

            totalReward = totalReward.add(totalAddNumber);
        }

        if (totalReward.compareTo(totalLimitReward) >= 0) {

            totalReward = totalLimitReward;
        }

        return SingleResponse.of(totalReward);

    }

    /**
     * 获取静态奖励比例
     */
    public SingleResponse<BigDecimal> getStaticRewardRate() {
        // 获取静态奖励比例
        LambdaQueryWrapper<SystemConfig> staticRewardRateQueryWrapper = new LambdaQueryWrapper<>();
        staticRewardRateQueryWrapper.eq(SystemConfig::getName, SystemConfigEnum.STATIC_REWARD_RATE.getCode());

        SystemConfig staticRewardRateSystemConfig = systemConfigMapper.selectOne(staticRewardRateQueryWrapper);
        if (staticRewardRateSystemConfig == null || staticRewardRateSystemConfig.getValue() == null) {
            return SingleResponse.buildFailure("静态奖励比例系统配置错误");
        }

        BigDecimal staticRewardRate = new BigDecimal(staticRewardRateSystemConfig.getValue());

        return SingleResponse.of(staticRewardRate);
    }

    /**
     * 获取动态奖励比例
     */
    public SingleResponse<BigDecimal> getDynamicRewardRate() {

        // 获取动态奖励比例
        LambdaQueryWrapper<SystemConfig> dynamicRewardRateQueryWrapper = new LambdaQueryWrapper<>();
        dynamicRewardRateQueryWrapper.eq(SystemConfig::getName, SystemConfigEnum.DYNAMIC_REWARD_RATE.getCode());

        SystemConfig dynamicRewardRateSystemConfig = systemConfigMapper.selectOne(dynamicRewardRateQueryWrapper);
        if (dynamicRewardRateSystemConfig == null || dynamicRewardRateSystemConfig.getValue() == null) {
            return SingleResponse.buildFailure("动态奖励比例系统配置错误");
        }

        BigDecimal dynamicRewardRate = new BigDecimal(dynamicRewardRateSystemConfig.getValue());

        return SingleResponse.of(dynamicRewardRate);

    }

    /**
     * 获取动态奖励推荐人比例
     */
    private SingleResponse<BigDecimal> getDynamicRewardRecommendRate() {
        // 获取矿机算力要求
        LambdaQueryWrapper<SystemConfig> dynamicRewardRecommendRateQueryWrapper = new LambdaQueryWrapper<>();
        dynamicRewardRecommendRateQueryWrapper.eq(SystemConfig::getName, SystemConfigEnum.DYNAMIC_REWARD_RECOMMEND_RATE.getCode());

        SystemConfig dynamicRewardRecommendRateSystemConfig = systemConfigMapper.selectOne(dynamicRewardRecommendRateQueryWrapper);
        if (dynamicRewardRecommendRateSystemConfig == null || dynamicRewardRecommendRateSystemConfig.getValue() == null) {
            return SingleResponse.buildFailure("动态奖励推荐人比例系统配置错误");
        }

        BigDecimal dynamicRewardRecommendRate = new BigDecimal(dynamicRewardRecommendRateSystemConfig.getValue());

        return SingleResponse.of(dynamicRewardRecommendRate);
    }


    public SingleResponse<Void> staticReward(BigDecimal totalComputingPower,
                                             BigDecimal totalReward,
                                             String dayTime,
                                             List<PurchaseMinerProjectReward> rewardList,
                                             SystemConfig ecoPrice) {

        Long endTime = LocalDate.parse(dayTime).plusDays(1).atStartOfDay()
                .atZone(ZoneId.systemDefault())  // 明确使用系统时区
                .toInstant()
                .toEpochMilli();

        LambdaQueryWrapper<PurchaseMinerProject> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(PurchaseMinerProject::getStatus,PurchaseMinerProjectStatus.SUCCESS.getCode());
        // 只查询指定日期及之前创建的矿机
        lambdaQueryWrapper.le(PurchaseMinerProject::getCreateTime, endTime);
        List<PurchaseMinerProject> purchaseMinerProjectList = purchaseMinerProjectMapper.selectList(lambdaQueryWrapper);

        // 获取静态奖励比例
        SingleResponse<BigDecimal> staticRewardRateResponse = getStaticRewardRate();
        if (!staticRewardRateResponse.isSuccess()) {
            return SingleResponse.buildFailure(staticRewardRateResponse.getErrMessage());
        }

        BigDecimal staticRewardRate = staticRewardRateResponse.getData();

        BigDecimal staticTotalReward = totalReward.multiply(staticRewardRate);

        log.info("【静态奖励发放】开始发放静态奖励，矿机总数: {}, 静态奖励总额: {}", purchaseMinerProjectList.size(), staticTotalReward);
        
        int qualifiedMinerCount = 0;
        int skippedMinerCount = 0;
        BigDecimal totalStaticRewardCalculated = BigDecimal.ZERO;
        BigDecimal totalStaticRewardActual = BigDecimal.ZERO;
        
        // 遍历所有矿机，计算静态奖励
        for (PurchaseMinerProject purchaseMinerProject : purchaseMinerProjectList) {

            // 检查用户是否有挖矿资格
            SingleResponse<Recommend> recommendResponse = checkRecommend(purchaseMinerProject.getWalletAddress());
            if (!recommendResponse.isSuccess()) {
                log.warn("【静态奖励发放】用户{}没有挖矿资格，原因: {}，跳过静态奖励发放", 
                    purchaseMinerProject.getWalletAddress(), recommendResponse.getErrMessage());
                
                // 记录没有获得奖励的日志
                recordRewardLog(purchaseMinerProject.getWalletAddress(), 
                    PurchaseMinerProjectRewardType.STATIC.getCode(), null,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 
                    "没有挖矿资格: " + recommendResponse.getErrMessage(),
                    purchaseMinerProject.getId(), purchaseMinerProject.getActualComputingPower(),
                    totalComputingPower.toString(), null, dayTime);
                skippedMinerCount++;
                continue;
            }

            qualifiedMinerCount++;
            Recommend recommend = recommendResponse.getData();

            // 获取每个矿机的静态奖励
            PurchaseMinerProjectReward reward = staticReward(purchaseMinerProject, recommend, totalComputingPower, staticTotalReward, dayTime, rewardList,ecoPrice);
            
            if (reward != null) {
                BigDecimal calculatedReward = new BigDecimal(purchaseMinerProject.getActualComputingPower())
                    .divide(totalComputingPower, 8, RoundingMode.DOWN)
                    .multiply(staticTotalReward);
                BigDecimal actualReward = new BigDecimal(reward.getReward());
                
                totalStaticRewardCalculated = totalStaticRewardCalculated.add(calculatedReward);
                totalStaticRewardActual = totalStaticRewardActual.add(actualReward);
            }

        }
        
        log.info("【静态奖励发放】静态奖励发放完成 - 总矿机数: {}, 有资格矿机数: {}, 跳过矿机数: {}, 计算奖励总额: {}, 实际发放总额: {}", 
                purchaseMinerProjectList.size(), qualifiedMinerCount, skippedMinerCount, totalStaticRewardCalculated, totalStaticRewardActual);

        return SingleResponse.buildSuccess();

    }

    /**
     * 获取每个矿机的静态奖励
     */
    public PurchaseMinerProjectReward staticReward(PurchaseMinerProject purchaseMinerProject,
                                                   Recommend recommend,
                                                   BigDecimal totalComputingPower,
                                                   BigDecimal staticTotalReward,
                                                   String dayTime,
                                                   List<PurchaseMinerProjectReward> rewardList,
                                                   SystemConfig ecoPrice) {
        // 计算静态奖励 = 每天总奖励数 * 静态奖励比例 * (用户矿机算力 / 总算力)


        if (purchaseMinerProject.getStatus().equals(PurchaseMinerProjectStatus.STOP.getCode())){

            log.info("矿机：{} ,状态为：{},已停止发放静态奖励",purchaseMinerProject.getId(),purchaseMinerProject.getStatus());
            return null;
        }


        String order = "ST" + System.currentTimeMillis();

        BigDecimal computingPower = new BigDecimal(purchaseMinerProject.getActualComputingPower());

        BigDecimal staticReward = computingPower.divide(totalComputingPower, 8, RoundingMode.DOWN).multiply(staticTotalReward);
        
        log.info("用户{}静态奖励计算: 矿机算力={}, 总算力={}, 计算奖励={}", 
            purchaseMinerProject.getWalletAddress(), computingPower, totalComputingPower, staticReward);

        BigDecimal finalReward = getFinalReward(Collections.singletonList(purchaseMinerProject),
                staticReward,
                recommend.getWalletAddress(),
                dayTime
        );
        
        // 如果最终奖励为0，直接记录日志并跳过账户操作
        if (finalReward.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("用户{}静态奖励为0，跳过账户操作", purchaseMinerProject.getWalletAddress());
            
            // 记录奖励日志 - 明确记录舍去原因
            String discardReason = staticReward.compareTo(BigDecimal.ZERO) > 0 ? 
                "矿机奖励上限限制" : "计算奖励为0";
            
            recordRewardLog(purchaseMinerProject.getWalletAddress(), 
                PurchaseMinerProjectRewardType.STATIC.getCode(), null,
                staticReward, BigDecimal.ZERO, staticReward, discardReason,
                purchaseMinerProject.getId(), purchaseMinerProject.getActualComputingPower(),
                totalComputingPower.toString(), order, dayTime);
            return null;
        }
        
        if (finalReward.compareTo(staticReward) < 0) {
            BigDecimal discardedReward = staticReward.subtract(finalReward);
            log.warn("用户{}静态奖励被限制，原奖励={}, 实际奖励={}, 舍去奖励={}", 
                purchaseMinerProject.getWalletAddress(), staticReward, finalReward, discardedReward);
            
            // 记录奖励日志
            recordRewardLog(purchaseMinerProject.getWalletAddress(), 
                PurchaseMinerProjectRewardType.STATIC.getCode(), null,
                staticReward, finalReward, discardedReward, "矿机奖励上限限制",
                purchaseMinerProject.getId(), purchaseMinerProject.getActualComputingPower(),
                totalComputingPower.toString(), order, dayTime);
        } else {
            // 记录正常奖励日志
            recordRewardLog(purchaseMinerProject.getWalletAddress(), 
                PurchaseMinerProjectRewardType.STATIC.getCode(), null,
                staticReward, finalReward, BigDecimal.ZERO, null,
                purchaseMinerProject.getId(), purchaseMinerProject.getActualComputingPower(),
                totalComputingPower.toString(), order, dayTime);
        }

        BigDecimal rewardPrice = finalReward.multiply(new BigDecimal(ecoPrice.getValue()));

        PurchaseMinerProjectReward purchaseMinerProjectReward = new PurchaseMinerProjectReward();
        purchaseMinerProjectReward.setOrder(order);
        purchaseMinerProjectReward.setPurchaseMinerProjectId(purchaseMinerProject.getId());
        purchaseMinerProjectReward.setReward(finalReward.toString());
        purchaseMinerProjectReward.setType(PurchaseMinerProjectRewardType.STATIC.getCode());
        purchaseMinerProjectReward.setWalletAddress(purchaseMinerProject.getWalletAddress().toLowerCase());
        purchaseMinerProjectReward.setLeaderWalletAddress(recommend.getLeaderWalletAddress());
        purchaseMinerProjectReward.setComputingPower(purchaseMinerProject.getActualComputingPower());
        purchaseMinerProjectReward.setTotalComputingPower(totalComputingPower.toString());
        purchaseMinerProjectReward.setRecommendWalletAddress(recommend.getRecommendWalletAddress());
        purchaseMinerProjectReward.setDayTime(dayTime);
        purchaseMinerProjectReward.setRewardPrice(rewardPrice.toString());
        purchaseMinerProjectReward.setCreateTime(System.currentTimeMillis());
        int insert = purchaseMinerProjectRewardMapper.insert(purchaseMinerProjectReward);

        if (insert <= 0) {
            log.info("用户{}发放静态奖励失败", purchaseMinerProject.getWalletAddress());
            return null;
        }

        rewardList.add(purchaseMinerProjectReward);

        log.info("用户{}发放静态奖励成功: 计算奖励={}, 实际发放={}", 
            purchaseMinerProject.getWalletAddress(), staticReward, finalReward);


        try {
            AccountStaticNumberCmd accountStaticNumberCmd = new AccountStaticNumberCmd();
            accountStaticNumberCmd.setWalletAddress(purchaseMinerProject.getWalletAddress());
            accountStaticNumberCmd.setNumber(finalReward.toString());
            accountStaticNumberCmd.setType(AccountType.ECO.getCode());
            accountStaticNumberCmd.setOrder(order);

            SingleResponse<Void> response = accountService.addStaticNumber(accountStaticNumberCmd);
            if (!response.isSuccess()) {
                log.info("用户{}发放静态奖励{}失败，调用账户服务失败", purchaseMinerProject.getWalletAddress(), staticReward);
            }

        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return purchaseMinerProjectReward;
    }

    /**
     * 获取所有用户的算力信息
     */
    private List<ComputingPowerDTO> getAllUsersComputingPower(String dayTime) {
        try {
            // 获取所有用户
            List<Recommend> allUsers = recommendMapper.selectList(new LambdaQueryWrapper<>());
            List<ComputingPowerDTO> computingPowerList = new ArrayList<>();
            
            for (Recommend user : allUsers) {
                SingleResponse<ComputingPowerDTO> response = computingPowerService.getComputingPowerInfo(
                    user.getWalletAddress(), dayTime);
                if (response.isSuccess() && response.getData() != null) {
                    computingPowerList.add(response.getData());
                }
            }
            
            return computingPowerList;
        } catch (Exception e) {
            log.error("获取所有用户算力信息失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 动态奖励
     */
    public SingleResponse<Void> dynamicReward(String dayTime,
                                              BigDecimal totalReward,
                                              List<ComputingPowerDTO> computingPowerList,
                                              List<PurchaseMinerProjectReward> rewardList,
                                              SystemConfig ecoPrice) {

        // 获取动态奖励比例
        SingleResponse<BigDecimal> dynamicRewardRateResponse = getDynamicRewardRate();
        if (!dynamicRewardRateResponse.isSuccess()) {
            return SingleResponse.buildFailure(dynamicRewardRateResponse.getErrMessage());
        }

        BigDecimal dynamicRewardRate = dynamicRewardRateResponse.getData();

        // 计算动态奖励总额
        BigDecimal dynamicTotalReward = totalReward.multiply(dynamicRewardRate);


        // 动态推荐奖励
        SingleResponse<Void> recommendRewardResponse = recommendReward(dynamicTotalReward, computingPowerList, dayTime, rewardList,ecoPrice);
        if (!recommendRewardResponse.isSuccess()) {
            return SingleResponse.buildFailure(recommendRewardResponse.getErrMessage());
        }

        // 动态基础奖励
        SingleResponse<Void> baseRewardResponse = baseReward(dynamicTotalReward, computingPowerList, dayTime, rewardList,ecoPrice);
        if (!baseRewardResponse.isSuccess()) {
            return SingleResponse.buildFailure(baseRewardResponse.getErrMessage());
        }

        // 动态新增奖励
        SingleResponse<Void> newRewardResponse = newReward(dynamicTotalReward, computingPowerList, dayTime, rewardList,ecoPrice);
        if (!newRewardResponse.isSuccess()) {
            return SingleResponse.buildFailure(newRewardResponse.getErrMessage());
        }

        return SingleResponse.buildSuccess();

    }

    /**
     * 动态推荐奖励
     */
    public SingleResponse<Void> recommendReward(BigDecimal dynamicTotalReward,
                                                List<ComputingPowerDTO> computingPowerList,
                                                String dayTime,
                                                List<PurchaseMinerProjectReward> rewardList,
                                                SystemConfig ecoPrice) {


        // 获取动态奖励推荐比例
        SingleResponse<BigDecimal> dynamicRewardRecommendRateResponse = getDynamicRewardRecommendRate();
        if (!dynamicRewardRecommendRateResponse.isSuccess()) {
            return SingleResponse.buildFailure(dynamicRewardRecommendRateResponse.getErrMessage());
        }
        // 获取动态奖励推荐比例
        BigDecimal dynamicRewardRecommendRate = dynamicRewardRecommendRateResponse.getData();

        // 计算动态推荐奖励总额
        BigDecimal dynamicRewardRecommendTotalReward = dynamicTotalReward.multiply(dynamicRewardRecommendRate);

        // 计算总直推算力
        BigDecimal totalDirectRecommendComputingPower = computingPowerList
                .stream()
                .map(ComputingPowerDTO::getDirectRecommendPower)
                .filter(Objects::nonNull)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        // 检查总直推算力是否为0
        if (totalDirectRecommendComputingPower.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("总直推算力为0，跳过动态推荐奖励发放");
            return SingleResponse.buildSuccess();
        }

        log.info("【动态推荐奖励发放】开始发放动态推荐奖励，用户总数: {}, 推荐奖励总额: {}", computingPowerList.size(), dynamicRewardRecommendTotalReward);
        
        int qualifiedUserCount = 0;
        int skippedUserCount = 0;
        int zeroPowerUserCount = 0;
        BigDecimal totalRecommendRewardCalculated = BigDecimal.ZERO;
        BigDecimal totalRecommendRewardActual = BigDecimal.ZERO;
        
        for (ComputingPowerDTO computingPower : computingPowerList) {

            if (computingPower.getDirectRecommendPower().compareTo(BigDecimal.ZERO) <= 0) {
                // 直推算力为0，跳过
                log.warn("【动态推荐奖励发放】用户{}直推算力为0，跳过动态推荐奖励发放", computingPower.getWalletAddress());
                
                // 记录没有获得奖励的日志
                recordRewardLog(computingPower.getWalletAddress(), 
                    PurchaseMinerProjectRewardType.DYNAMIC.getCode(), 
                    PurchaseMinerProjectDynamicRewardType.RECOMMEND.getCode(),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 
                    "直推算力为0",
                    null, "0", totalDirectRecommendComputingPower.toString(), null, dayTime);
                zeroPowerUserCount++;
                continue;
            }

            // 检查用户是否有挖矿资格
            SingleResponse<Recommend> recommendResponse = checkRecommend(computingPower.getWalletAddress());
            if (!recommendResponse.isSuccess()) {
                log.warn("【动态推荐奖励发放】用户{}没有挖矿资格，原因: {}，跳过动态推荐奖励发放", 
                    computingPower.getWalletAddress(), recommendResponse.getErrMessage());
                
                // 记录没有获得奖励的日志
                recordRewardLog(computingPower.getWalletAddress(), 
                    PurchaseMinerProjectRewardType.DYNAMIC.getCode(), 
                    PurchaseMinerProjectDynamicRewardType.RECOMMEND.getCode(),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 
                    "没有挖矿资格: " + recommendResponse.getErrMessage(),
                    null, computingPower.getDirectRecommendPower().toString(),
                    totalDirectRecommendComputingPower.toString(), null, dayTime);
                skippedUserCount++;
                continue;
            }

            qualifiedUserCount++;
            Recommend recommend = recommendResponse.getData();

            // 计算动态推荐奖励
            BigDecimal calculatedReward = computingPower.getDirectRecommendPower()
                .divide(totalDirectRecommendComputingPower, 8, RoundingMode.DOWN)
                .multiply(dynamicRewardRecommendTotalReward);
            totalRecommendRewardCalculated = totalRecommendRewardCalculated.add(calculatedReward);
            
            recommendReward(computingPower, dynamicRewardRecommendTotalReward, totalDirectRecommendComputingPower, recommend, dayTime, rewardList,ecoPrice);

        }
        
        // 计算实际发放的推荐奖励
        totalRecommendRewardActual = rewardList.stream()
                .filter(r -> PurchaseMinerProjectRewardType.DYNAMIC.getCode().equals(r.getType()))
                .filter(r -> PurchaseMinerProjectDynamicRewardType.RECOMMEND.getCode().equals(r.getRewardType()))
                .map(r -> new BigDecimal(r.getReward()))
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
        
        log.info("【动态推荐奖励发放】动态推荐奖励发放完成 - 总用户数: {}, 有资格用户数: {}, 跳过用户数: {}, 零算力用户数: {}, 计算奖励总额: {}, 实际发放总额: {}", 
                computingPowerList.size(), qualifiedUserCount, skippedUserCount, zeroPowerUserCount, totalRecommendRewardCalculated, totalRecommendRewardActual);
        return SingleResponse.buildSuccess();

    }


    /**
     * 动态推荐奖励
     */
    public SingleResponse<Void> recommendReward(ComputingPowerDTO computingPower,
                                                BigDecimal dynamicRewardRecommendTotalReward,
                                                BigDecimal totalDirectRecommendComputingPower,
                                                Recommend recommend,
                                                String dayTime,
                                                List<PurchaseMinerProjectReward> rewardList,
                                                SystemConfig ecoPrice) {

        // 计算动态推荐奖励 = 推荐总奖励 * (用户直推算力 / 总直推算力)

        String order = "DTR" + System.currentTimeMillis();
        //直推总算力
        BigDecimal directRecommendComputingPower = computingPower.getDirectRecommendPower();

        BigDecimal recommendReward = directRecommendComputingPower.divide(totalDirectRecommendComputingPower, 8, RoundingMode.DOWN).multiply(dynamicRewardRecommendTotalReward);
        
        log.info("用户{}动态推荐奖励计算: 直推算力={}, 总直推算力={}, 计算奖励={}", 
            computingPower.getWalletAddress(), directRecommendComputingPower, totalDirectRecommendComputingPower, recommendReward);

        BigDecimal finalReward = getFinalReward(new ArrayList<>(),
                recommendReward,
                recommend.getWalletAddress(),
                dayTime);
        
        // 如果最终奖励为0，直接记录日志并跳过账户操作
        if (finalReward.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("用户{}动态推荐奖励为0，跳过账户操作", computingPower.getWalletAddress());
            
            // 记录奖励日志 - 明确记录舍去原因
            String discardReason = recommendReward.compareTo(BigDecimal.ZERO) > 0 ? 
                "矿机奖励上限限制" : "计算奖励为0";
            
            recordRewardLog(computingPower.getWalletAddress(), 
                PurchaseMinerProjectRewardType.DYNAMIC.getCode(), 
                PurchaseMinerProjectDynamicRewardType.RECOMMEND.getCode(),
                recommendReward, BigDecimal.ZERO, recommendReward, discardReason,
                null, directRecommendComputingPower.toString(),
                totalDirectRecommendComputingPower.toString(), order, dayTime);
            return SingleResponse.buildSuccess();
        }
        
        if (finalReward.compareTo(recommendReward) < 0) {
            BigDecimal discardedReward = recommendReward.subtract(finalReward);
            log.warn("用户{}动态推荐奖励被限制，原奖励={}, 实际奖励={}, 舍去奖励={}", 
                computingPower.getWalletAddress(), recommendReward, finalReward, discardedReward);
            
            // 记录奖励日志
            recordRewardLog(computingPower.getWalletAddress(), 
                PurchaseMinerProjectRewardType.DYNAMIC.getCode(), 
                PurchaseMinerProjectDynamicRewardType.RECOMMEND.getCode(),
                recommendReward, finalReward, discardedReward, "矿机奖励上限限制",
                null, directRecommendComputingPower.toString(),
                totalDirectRecommendComputingPower.toString(), order, dayTime);
        } else {
            // 记录正常奖励日志
            recordRewardLog(computingPower.getWalletAddress(), 
                PurchaseMinerProjectRewardType.DYNAMIC.getCode(), 
                PurchaseMinerProjectDynamicRewardType.RECOMMEND.getCode(),
                recommendReward, finalReward, BigDecimal.ZERO, null,
                null, directRecommendComputingPower.toString(),
                totalDirectRecommendComputingPower.toString(), order, dayTime);
        }

        BigDecimal price = finalReward.multiply(new BigDecimal(ecoPrice.getValue()));

        PurchaseMinerProjectReward purchaseMinerProjectReward = new PurchaseMinerProjectReward();
        purchaseMinerProjectReward.setOrder(order);
        purchaseMinerProjectReward.setReward(finalReward.toString());
        purchaseMinerProjectReward.setType(PurchaseMinerProjectRewardType.DYNAMIC.getCode());
        purchaseMinerProjectReward.setWalletAddress(computingPower.getWalletAddress().toLowerCase());
        purchaseMinerProjectReward.setLeaderWalletAddress(recommend.getLeaderWalletAddress());
        purchaseMinerProjectReward.setRecommendWalletAddress(recommend.getRecommendWalletAddress());
        purchaseMinerProjectReward.setTotalComputingPower(totalDirectRecommendComputingPower.toString());
        purchaseMinerProjectReward.setComputingPower(directRecommendComputingPower.toString());
        purchaseMinerProjectReward.setRewardType(PurchaseMinerProjectDynamicRewardType.RECOMMEND.getCode());
        purchaseMinerProjectReward.setDayTime(dayTime);
        purchaseMinerProjectReward.setCreateTime(System.currentTimeMillis());
        purchaseMinerProjectReward.setRewardPrice(price.toString());
        int insert = purchaseMinerProjectRewardMapper.insert(purchaseMinerProjectReward);

        if (insert <= 0) {
            log.info("用户{}发放动态推荐奖励失败", computingPower.getWalletAddress());
            return SingleResponse.buildFailure("发放动态推荐奖励失败");
        }

        rewardList.add(purchaseMinerProjectReward);

        log.info("用户{}发放动态推荐奖励成功: 计算奖励={}, 实际发放={}", 
            computingPower.getWalletAddress(), recommendReward, finalReward);

        try {
            AccountDynamicNumberCmd accountDynamicNumberCmd = new AccountDynamicNumberCmd();
            accountDynamicNumberCmd.setWalletAddress(computingPower.getWalletAddress());
            accountDynamicNumberCmd.setNumber(finalReward.toString());
            accountDynamicNumberCmd.setType(AccountType.ECO.getCode());
            accountDynamicNumberCmd.setOrder(order);

            SingleResponse<Void> response = accountService.addDynamicNumber(accountDynamicNumberCmd);
            if (!response.isSuccess()) {
                log.info("用户{}发放动态推荐奖励{}失败，调用账户服务失败", computingPower.getWalletAddress(), recommendReward);
            }

        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return SingleResponse.buildSuccess();
    }

    /**
     * 获取动态奖励小区比例
     */
    public SingleResponse<BigDecimal> getDynamicBaseRewardRate() {

        LambdaQueryWrapper<SystemConfig> dynamicRewardBaseRateQueryWrapper = new LambdaQueryWrapper<>();
        dynamicRewardBaseRateQueryWrapper.eq(SystemConfig::getName, SystemConfigEnum.DYNAMIC_REWARD_BASE_RATE.getCode());

        SystemConfig dynamicRewardBaseRateSystemConfig = systemConfigMapper.selectOne(dynamicRewardBaseRateQueryWrapper);
        if (dynamicRewardBaseRateSystemConfig == null || dynamicRewardBaseRateSystemConfig.getValue() == null) {
            return SingleResponse.buildFailure("动态奖励小区奖励比例系统配置错误");
        }

        BigDecimal dynamicRewardBaseRate = new BigDecimal(dynamicRewardBaseRateSystemConfig.getValue());

        return SingleResponse.of(dynamicRewardBaseRate);
    }


    /**
     * 动态小区奖励
     */
    public SingleResponse<Void> baseReward(BigDecimal dynamicTotalReward,
                                           List<ComputingPowerDTO> computingPowerList,
                                           String dayTime,
                                           List<PurchaseMinerProjectReward> rewardList,
                                           SystemConfig ecoPrice) {

        // 获取动态奖励小区比例
        SingleResponse<BigDecimal> dynamicRewardBaseRateResponse = getDynamicBaseRewardRate();
        if (!dynamicRewardBaseRateResponse.isSuccess()) {
            return SingleResponse.buildFailure(dynamicRewardBaseRateResponse.getErrMessage());
        }
        // 获取动态奖励小区比例
        BigDecimal dynamicRewardBaseRate = dynamicRewardBaseRateResponse.getData();

        // 计算动态小区奖励总额
        BigDecimal dynamicBaseTotalReward = dynamicTotalReward.multiply(dynamicRewardBaseRate);

        // 获取总小区算力
        BigDecimal totalMinComputingPower = computingPowerList.stream()
                .map(ComputingPowerDTO::getMinPower)
                .filter(Objects::nonNull)  // 过滤null值
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 检查总小区算力是否为0
        if (totalMinComputingPower.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("总小区算力为0，跳过动态小区奖励发放");
            return SingleResponse.buildSuccess();
        }

        log.info("开始发放动态小区奖励，用户总数: {}, 小区奖励总额: {}", computingPowerList.size(), dynamicBaseTotalReward);
        
        for (ComputingPowerDTO computingPower : computingPowerList) {

            // 检查用户是否有挖矿资格
            SingleResponse<Recommend> recommendResponse = checkRecommend(computingPower.getWalletAddress());
            if (!recommendResponse.isSuccess()) {
                log.warn("用户{}没有挖矿资格，原因: {}，跳过动态小区奖励发放", 
                    computingPower.getWalletAddress(), recommendResponse.getErrMessage());
                
                // 记录没有获得奖励的日志
                recordRewardLog(computingPower.getWalletAddress(), 
                    PurchaseMinerProjectRewardType.DYNAMIC.getCode(), 
                    PurchaseMinerProjectDynamicRewardType.BASE.getCode(),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 
                    "没有挖矿资格: " + recommendResponse.getErrMessage(),
                    null, computingPower.getMinPower().toString(),
                    totalMinComputingPower.toString(), null, dayTime);
                continue;
            }

            Recommend recommend = recommendResponse.getData();

            // 计算动态小区奖励
            baseReward(computingPower, dynamicBaseTotalReward, totalMinComputingPower, recommend, dayTime, rewardList,ecoPrice);

        }


        return SingleResponse.buildSuccess();
    }


    public SingleResponse<Void> baseReward(ComputingPowerDTO computingPower,
                                           BigDecimal dynamicBaseTotalReward,
                                           BigDecimal totalMinComputingPower,
                                           Recommend recommend,
                                           String dayTime,
                                           List<PurchaseMinerProjectReward> rewardList,
                                           SystemConfig ecoPrice) {

        // 计算动态小区奖励 = 动态小区奖励总额 * (用户小区总算力 / 总算力)

        String order = "DTB" + System.currentTimeMillis();

        // 检查minComputingPower是否为null或0
        BigDecimal minComputingPower = computingPower.getMinPower();
        if (minComputingPower == null) {
            log.warn("用户{}的小区算力为null，跳过动态小区奖励", computingPower.getWalletAddress());
            
            // 记录没有获得奖励的日志
            recordRewardLog(computingPower.getWalletAddress(), 
                PurchaseMinerProjectRewardType.DYNAMIC.getCode(), 
                PurchaseMinerProjectDynamicRewardType.BASE.getCode(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 
                "小区算力为null",
                null, "0", totalMinComputingPower.toString(), null, dayTime);
            return SingleResponse.buildSuccess();
        }
        
        // 检查算力是否为0
        if (minComputingPower.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("用户{}的小区算力为0，跳过动态小区奖励", computingPower.getWalletAddress());
            
            // 记录没有获得奖励的日志
            recordRewardLog(computingPower.getWalletAddress(), 
                PurchaseMinerProjectRewardType.DYNAMIC.getCode(), 
                PurchaseMinerProjectDynamicRewardType.BASE.getCode(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 
                "小区算力为0",
                null, "0", totalMinComputingPower.toString(), null, dayTime);
            return SingleResponse.buildSuccess();
        }
        // 动态奖励
        BigDecimal baseReward = minComputingPower
                .divide(totalMinComputingPower, 8, RoundingMode.DOWN)
                .multiply(dynamicBaseTotalReward);
        
        log.info("用户{}动态小区奖励计算: 小区算力={}, 总小区算力={}, 计算奖励={}", 
            computingPower.getWalletAddress(), minComputingPower, totalMinComputingPower, baseReward);

        BigDecimal finalReward = getFinalReward(new ArrayList<>(),
                baseReward,
                recommend.getWalletAddress(),
                dayTime);
        
        // 如果最终奖励为0，直接记录日志并跳过账户操作
        if (finalReward.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("用户{}动态小区奖励为0，跳过账户操作", computingPower.getWalletAddress());
            
            // 记录奖励日志 - 明确记录舍去原因
            String discardReason = baseReward.compareTo(BigDecimal.ZERO) > 0 ? 
                "矿机奖励上限限制" : "计算奖励为0";
            
            recordRewardLog(computingPower.getWalletAddress(), 
                PurchaseMinerProjectRewardType.DYNAMIC.getCode(), 
                PurchaseMinerProjectDynamicRewardType.BASE.getCode(),
                baseReward, BigDecimal.ZERO, baseReward, discardReason,
                null, minComputingPower.toString(),
                totalMinComputingPower.toString(), order, dayTime);
            return SingleResponse.buildSuccess();
        }
        
        if (finalReward.compareTo(baseReward) < 0) {
            BigDecimal discardedReward = baseReward.subtract(finalReward);
            log.warn("用户{}动态小区奖励被限制，原奖励={}, 实际奖励={}, 舍去奖励={}", 
                computingPower.getWalletAddress(), baseReward, finalReward, discardedReward);
            
            // 记录奖励日志
            recordRewardLog(computingPower.getWalletAddress(), 
                PurchaseMinerProjectRewardType.DYNAMIC.getCode(), 
                PurchaseMinerProjectDynamicRewardType.BASE.getCode(),
                baseReward, finalReward, discardedReward, "矿机奖励上限限制",
                null, minComputingPower.toString(),
                totalMinComputingPower.toString(), order, dayTime);
        } else {
            // 记录正常奖励日志
            recordRewardLog(computingPower.getWalletAddress(), 
                PurchaseMinerProjectRewardType.DYNAMIC.getCode(), 
                PurchaseMinerProjectDynamicRewardType.BASE.getCode(),
                baseReward, finalReward, BigDecimal.ZERO, null,
                null, minComputingPower.toString(),
                totalMinComputingPower.toString(), order, dayTime);
        }

        BigDecimal price = finalReward.multiply(new BigDecimal(ecoPrice.getValue()));

        PurchaseMinerProjectReward purchaseMinerProjectReward = new PurchaseMinerProjectReward();
        purchaseMinerProjectReward.setOrder(order);
        purchaseMinerProjectReward.setRewardType(PurchaseMinerProjectDynamicRewardType.BASE.getCode());
        purchaseMinerProjectReward.setReward(finalReward.toString());
        purchaseMinerProjectReward.setType(PurchaseMinerProjectRewardType.DYNAMIC.getCode());
        purchaseMinerProjectReward.setDayTime(dayTime);
        purchaseMinerProjectReward.setComputingPower(computingPower.getTotalPower().toString());
        purchaseMinerProjectReward.setTotalComputingPower(totalMinComputingPower.toString());
        purchaseMinerProjectReward.setMinComputingPower(computingPower.getMinPower().toString());
        purchaseMinerProjectReward.setMaxComputingPower(computingPower.getMaxPower().toString());
        purchaseMinerProjectReward.setWalletAddress(recommend.getWalletAddress().toLowerCase());
        purchaseMinerProjectReward.setRecommendWalletAddress(recommend.getRecommendWalletAddress());
        purchaseMinerProjectReward.setLeaderWalletAddress(recommend.getLeaderWalletAddress());
        purchaseMinerProjectReward.setRewardPrice(price.toString());
        purchaseMinerProjectReward.setCreateTime(System.currentTimeMillis());

        int insert = purchaseMinerProjectRewardMapper.insert(purchaseMinerProjectReward);
        if (insert <= 0) {
            log.info("用户{}发放动态小区奖励失败", computingPower.getWalletAddress());
            return SingleResponse.buildFailure("发放动态小区奖励失败");
        }

        rewardList.add(purchaseMinerProjectReward);

        log.info("用户{}发放动态小区奖励{}成功", computingPower.getWalletAddress(), baseReward);

        try {
            AccountDynamicNumberCmd accountDynamicNumberCmd = new AccountDynamicNumberCmd();
            accountDynamicNumberCmd.setWalletAddress(computingPower.getWalletAddress());
            accountDynamicNumberCmd.setNumber(finalReward.toString());
            accountDynamicNumberCmd.setType(AccountType.ECO.getCode());
            accountDynamicNumberCmd.setOrder(order);

            SingleResponse<Void> response = accountService.addDynamicNumber(accountDynamicNumberCmd);
            if (!response.isSuccess()) {
                log.info("用户{}发放动态小区奖励{}失败，调用账户服务失败", computingPower.getWalletAddress(), baseReward);
            }

        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return SingleResponse.buildSuccess();
    }


    /**
     * 获取动态奖励新增奖励比例
     */
    private SingleResponse<BigDecimal> getDynamicRewardNewRate() {
        // 获取矿机算力要求
        LambdaQueryWrapper<SystemConfig> dynamicRewardNewRateQueryWrapper = new LambdaQueryWrapper<>();
        dynamicRewardNewRateQueryWrapper.eq(SystemConfig::getName, SystemConfigEnum.DYNAMIC_REWARD_NEW_RATE.getCode());

        SystemConfig dynamicRewardNewRateSystemConfig = systemConfigMapper.selectOne(dynamicRewardNewRateQueryWrapper);
        if (dynamicRewardNewRateSystemConfig == null || dynamicRewardNewRateSystemConfig.getValue() == null) {
            return SingleResponse.buildFailure("动态奖励新增奖比例系统配置错误");
        }

        BigDecimal dynamicRewardNewRate = new BigDecimal(dynamicRewardNewRateSystemConfig.getValue());

        return SingleResponse.of(dynamicRewardNewRate);
    }


    /**
     * 动态新增奖励
     */
    public SingleResponse<Void> newReward(BigDecimal dynamicTotalReward,
                                          List<ComputingPowerDTO> computingPowerList,
                                          String dayTime,
                                          List<PurchaseMinerProjectReward> rewardList,
                                          SystemConfig ecoPrice) {

        // 获取动态奖励小区比例
        SingleResponse<BigDecimal> dynamicRewardNewRateResponse = getDynamicRewardNewRate();
        if (!dynamicRewardNewRateResponse.isSuccess()) {
            return SingleResponse.buildFailure(dynamicRewardNewRateResponse.getErrMessage());
        }
        // 获取动态奖励小区比例
        BigDecimal dynamicRewardNewRate = dynamicRewardNewRateResponse.getData();

        // 计算动态小区奖励总额
        BigDecimal dynamicNewTotalReward = dynamicTotalReward.multiply(dynamicRewardNewRate);

        // 获取总新增算力
        BigDecimal totalNewComputingPower = computingPowerList.stream()
                .map(ComputingPowerDTO::getNewPower)
                .filter(Objects::nonNull)  // 过滤null值
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 检查总新增算力是否为0
        if (totalNewComputingPower.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("总新增算力为0，跳过动态新增奖励发放");
            return SingleResponse.buildSuccess();
        }

        log.info("开始发放动态新增奖励，用户总数: {}, 新增奖励总额: {}", computingPowerList.size(), dynamicNewTotalReward);
        
        for (ComputingPowerDTO computingPower : computingPowerList) {

            // 检查用户是否有挖矿资格
            SingleResponse<Recommend> recommendResponse = checkRecommend(computingPower.getWalletAddress());
            if (!recommendResponse.isSuccess()) {
                log.warn("用户{}没有挖矿资格，原因: {}，跳过动态新增奖励发放", 
                    computingPower.getWalletAddress(), recommendResponse.getErrMessage());
                
                // 记录没有获得奖励的日志
                recordRewardLog(computingPower.getWalletAddress(), 
                    PurchaseMinerProjectRewardType.DYNAMIC.getCode(), 
                    PurchaseMinerProjectDynamicRewardType.NEW.getCode(),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 
                    "没有挖矿资格: " + recommendResponse.getErrMessage(),
                    null, computingPower.getNewPower().toString(),
                    totalNewComputingPower.toString(), null, dayTime);
                continue;
            }

            Recommend recommend = recommendResponse.getData();

            // 计算动态小区奖励
            newReward(computingPower, dynamicNewTotalReward, totalNewComputingPower, recommend, dayTime, rewardList,ecoPrice);

        }


        return SingleResponse.buildSuccess();
    }

    public SingleResponse<Void> newReward(ComputingPowerDTO computingPower,
                                          BigDecimal dynamicNewTotalReward,
                                          BigDecimal totalNewComputingPower,
                                          Recommend recommend,
                                          String dayTime,
                                          List<PurchaseMinerProjectReward> rewardList,
                                          SystemConfig ecoPrice) {

        // 计算动态小区奖励 = 动态小区奖励总额 * (用户小区总算力 / 总算力)

        String order = "DTN" + System.currentTimeMillis();

        // 检查newComputingPower是否为null或0
        BigDecimal newComputingPower = computingPower.getNewPower();
        if (newComputingPower == null) {
            log.warn("用户{}的新增算力为null，跳过动态新增奖励", computingPower.getWalletAddress());
            
            // 记录没有获得奖励的日志
            recordRewardLog(computingPower.getWalletAddress(), 
                PurchaseMinerProjectRewardType.DYNAMIC.getCode(), 
                PurchaseMinerProjectDynamicRewardType.NEW.getCode(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 
                "新增算力为null",
                null, "0", totalNewComputingPower.toString(), null, dayTime);
            return SingleResponse.buildSuccess();
        }
        
        // 检查算力是否为0
        if (newComputingPower.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("用户{}的新增算力为0，跳过动态新增奖励", computingPower.getWalletAddress());
            
            // 记录没有获得奖励的日志
            recordRewardLog(computingPower.getWalletAddress(), 
                PurchaseMinerProjectRewardType.DYNAMIC.getCode(), 
                PurchaseMinerProjectDynamicRewardType.NEW.getCode(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 
                "新增算力为0",
                null, "0", totalNewComputingPower.toString(), null, dayTime);
            return SingleResponse.buildSuccess();
        }
        
        // 动态奖励
        BigDecimal newReward = newComputingPower
                .divide(totalNewComputingPower, 8, RoundingMode.DOWN)
                .multiply(dynamicNewTotalReward);
        
        log.info("用户{}动态新增奖励计算: 新增算力={}, 总新增算力={}, 计算奖励={}", 
            computingPower.getWalletAddress(), newComputingPower, totalNewComputingPower, newReward);

        BigDecimal finalReward = getFinalReward(new ArrayList<>(),
                newReward,
                recommend.getWalletAddress(),
                dayTime
        );
        
        // 如果最终奖励为0，直接记录日志并跳过账户操作
        if (finalReward.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("用户{}动态新增奖励为0，跳过账户操作", computingPower.getWalletAddress());
            
            // 记录奖励日志 - 明确记录舍去原因
            String discardReason = newReward.compareTo(BigDecimal.ZERO) > 0 ? 
                "矿机奖励上限限制" : "计算奖励为0";
            
            recordRewardLog(computingPower.getWalletAddress(), 
                PurchaseMinerProjectRewardType.DYNAMIC.getCode(), 
                PurchaseMinerProjectDynamicRewardType.NEW.getCode(),
                newReward, BigDecimal.ZERO, newReward, discardReason,
                null, newComputingPower.toString(),
                totalNewComputingPower.toString(), order, dayTime);
            return SingleResponse.buildSuccess();
        }
        
        if (finalReward.compareTo(newReward) < 0) {
            BigDecimal discardedReward = newReward.subtract(finalReward);
            log.warn("用户{}动态新增奖励被限制，原奖励={}, 实际奖励={}, 舍去奖励={}", 
                computingPower.getWalletAddress(), newReward, finalReward, discardedReward);
            
            // 记录奖励日志
            recordRewardLog(computingPower.getWalletAddress(), 
                PurchaseMinerProjectRewardType.DYNAMIC.getCode(), 
                PurchaseMinerProjectDynamicRewardType.NEW.getCode(),
                newReward, finalReward, discardedReward, "矿机奖励上限限制",
                null, newComputingPower.toString(),
                totalNewComputingPower.toString(), order, dayTime);
        } else {
            // 记录正常奖励日志
            recordRewardLog(computingPower.getWalletAddress(), 
                PurchaseMinerProjectRewardType.DYNAMIC.getCode(), 
                PurchaseMinerProjectDynamicRewardType.NEW.getCode(),
                newReward, finalReward, BigDecimal.ZERO, null,
                null, newComputingPower.toString(),
                totalNewComputingPower.toString(), order, dayTime);
        }

        BigDecimal price = finalReward.multiply(new BigDecimal(ecoPrice.getValue()));

        PurchaseMinerProjectReward purchaseMinerProjectReward = new PurchaseMinerProjectReward();
        purchaseMinerProjectReward.setOrder(order);
        purchaseMinerProjectReward.setRewardType(PurchaseMinerProjectDynamicRewardType.NEW.getCode());
        purchaseMinerProjectReward.setReward(finalReward.toString());
        purchaseMinerProjectReward.setType(PurchaseMinerProjectRewardType.DYNAMIC.getCode());
        purchaseMinerProjectReward.setDayTime(dayTime);
        purchaseMinerProjectReward.setComputingPower(computingPower.getNewPower().toString());
        purchaseMinerProjectReward.setTotalComputingPower(totalNewComputingPower.toString());
        purchaseMinerProjectReward.setWalletAddress(recommend.getWalletAddress().toLowerCase());
        purchaseMinerProjectReward.setMinComputingPower(computingPower.getMinPower().toString());
        purchaseMinerProjectReward.setMaxComputingPower(computingPower.getMaxPower().toString());
        purchaseMinerProjectReward.setRecommendWalletAddress(recommend.getRecommendWalletAddress());
        purchaseMinerProjectReward.setLeaderWalletAddress(recommend.getLeaderWalletAddress());
        purchaseMinerProjectReward.setRewardPrice(price.toString());
        purchaseMinerProjectReward.setCreateTime(System.currentTimeMillis());

        int insert = purchaseMinerProjectRewardMapper.insert(purchaseMinerProjectReward);
        if (insert <= 0) {
            log.info("用户{}发放动态新增奖励失败", computingPower.getWalletAddress());
            return SingleResponse.buildFailure("发放动态小区奖励失败");
        }

        rewardList.add(purchaseMinerProjectReward);

        log.info("用户{}发放动态新增奖励{}成功", computingPower.getWalletAddress(), newReward);

        try {
            AccountDynamicNumberCmd accountDynamicNumberCmd = new AccountDynamicNumberCmd();
            accountDynamicNumberCmd.setWalletAddress(computingPower.getWalletAddress());
            accountDynamicNumberCmd.setNumber(finalReward.toString());
            accountDynamicNumberCmd.setType(AccountType.ECO.getCode());
            accountDynamicNumberCmd.setOrder(order);

            SingleResponse<Void> response = accountService.addDynamicNumber(accountDynamicNumberCmd);
            if (!response.isSuccess()) {
                log.info("用户{}发放动态新增奖励{}失败，调用账户服务失败", computingPower.getWalletAddress(), newReward);
            }

        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return SingleResponse.buildSuccess();
    }

    /**
     * 添加奖励统计
     */
    public SingleResponse<Void> addRewardStatisticsLog(String dayTime, List<PurchaseMinerProjectReward> rewardList) {


        BigDecimal totalReward = rewardList.stream()
                .map(PurchaseMinerProjectReward::getReward)
                .map(BigDecimal::new)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);


        BigDecimal totalStaticReward = rewardList.stream()
                .filter(x -> x.getType().equals(PurchaseMinerProjectRewardType.STATIC.getCode()))
                .map(PurchaseMinerProjectReward::getReward)
                .filter(Objects::nonNull)  // 过滤null值
                .filter(reward -> !reward.trim().isEmpty())  // 过滤空字符串
                .map(BigDecimal::new)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);


        BigDecimal totalDynamicReward = rewardList.stream()
                .filter(x -> x.getType().equals(PurchaseMinerProjectRewardType.DYNAMIC.getCode()))
                .map(PurchaseMinerProjectReward::getReward)
                .filter(Objects::nonNull)  // 过滤null值
                .filter(reward -> !reward.trim().isEmpty())  // 过滤空字符串
                .map(BigDecimal::new)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalDynamicRecommendReward = rewardList.stream()
                .filter(x -> x.getType().equals(PurchaseMinerProjectRewardType.DYNAMIC.getCode()))
                .filter(x -> x.getRewardType().equals(PurchaseMinerProjectDynamicRewardType.RECOMMEND.getCode()))
                .map(PurchaseMinerProjectReward::getReward)
                .filter(Objects::nonNull)  // 过滤null值
                .filter(reward -> !reward.trim().isEmpty())  // 过滤空字符串
                .map(BigDecimal::new)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);


        BigDecimal totalDynamicBaseReward = rewardList.stream()
                .filter(x -> x.getType().equals(PurchaseMinerProjectRewardType.DYNAMIC.getCode()))
                .filter(x -> x.getRewardType().equals(PurchaseMinerProjectDynamicRewardType.BASE.getCode()))
                .map(PurchaseMinerProjectReward::getReward)
                .filter(Objects::nonNull)  // 过滤null值
                .filter(reward -> !reward.trim().isEmpty())  // 过滤空字符串
                .map(BigDecimal::new)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalDynamicNewReward = rewardList.stream()
                .filter(x -> x.getType().equals(PurchaseMinerProjectRewardType.DYNAMIC.getCode()))
                .filter(x -> x.getRewardType().equals(PurchaseMinerProjectDynamicRewardType.NEW.getCode()))
                .map(PurchaseMinerProjectReward::getReward)
                .filter(Objects::nonNull)  // 过滤null值
                .filter(reward -> !reward.trim().isEmpty())  // 过滤空字符串
                .map(BigDecimal::new)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);


        LambdaQueryWrapper<RewardStatisticsLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RewardStatisticsLog::getDayTime, dayTime);

        rewardStatisticsLogMapper.delete(queryWrapper);

        RewardStatisticsLog rewardStatisticsLog = new RewardStatisticsLog();

        rewardStatisticsLog.setDayTime(dayTime);
        rewardStatisticsLog.setTotalReward(totalReward.toString());
        rewardStatisticsLog.setTotalStaticReward(totalStaticReward.toString());
        rewardStatisticsLog.setTotalDynamicReward(totalDynamicReward.toString());
        rewardStatisticsLog.setTotalRecommendReward(totalDynamicRecommendReward.toString());
        rewardStatisticsLog.setTotalBaseReward(totalDynamicBaseReward.toString());
        rewardStatisticsLog.setTotalNewReward(totalDynamicNewReward.toString());

        rewardStatisticsLogMapper.insert(rewardStatisticsLog);

        return SingleResponse.buildSuccess();

    }

    /**
     * 获取最终奖励数量（修复版本）
     * @param purchaseMinerProjectList 矿机项目列表
     * @param reward 当前奖励数量
     * @param walletAddress 钱包地址
     * @return 实际发放的奖励数量
     */
    public BigDecimal getFinalReward(List<PurchaseMinerProject> purchaseMinerProjectList,
                                     BigDecimal reward,
                                     String walletAddress,
                                     String dayTime) {
        
        log.info("【奖励上限检查】开始检查用户{}的奖励上限，原奖励: {}", walletAddress, reward);
        
        // 获取ECO价格配置
        LambdaQueryWrapper<SystemConfig> systemConfigLambdaQueryWrapper = new LambdaQueryWrapper<>();
        systemConfigLambdaQueryWrapper.eq(SystemConfig::getName, SystemConfigEnum.ECO_PRICE.getCode());

        SystemConfig systemConfig = systemConfigMapper.selectOne(systemConfigLambdaQueryWrapper);
        if (Objects.isNull(systemConfig)) {
            log.warn("【奖励上限检查】用户{}未设置ECO价格，无法计算奖励上限", walletAddress);
            return BigDecimal.ZERO;
        }

        BigDecimal price = new BigDecimal(systemConfig.getValue());
        log.info("【奖励上限检查】用户{}使用ECO价格: {}", walletAddress, price);

        // 如果没有传入矿机列表，则查询用户的矿机
        if (CollectionUtils.isEmpty(purchaseMinerProjectList)) {
            LambdaQueryWrapper<PurchaseMinerProject> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PurchaseMinerProject::getWalletAddress, walletAddress);
            queryWrapper.eq(PurchaseMinerProject::getStatus, PurchaseMinerProjectStatus.SUCCESS.getCode());
            purchaseMinerProjectList = purchaseMinerProjectMapper.selectList(queryWrapper);
        }

        if (CollectionUtils.isEmpty(purchaseMinerProjectList)) {
            log.warn("【奖励上限检查】用户{}没有可用的矿机，无法发放奖励", walletAddress);
            return BigDecimal.ZERO;
        }

        log.info("【奖励上限检查】用户{}有{}台可用矿机", walletAddress, purchaseMinerProjectList.size());

        BigDecimal actualReward = BigDecimal.ZERO;  // 实际发放的奖励数量
        BigDecimal remainingReward = reward;  // 剩余待分配的奖励
        int processedMinerCount = 0;
        int maxedOutMinerCount = 0;

        for (PurchaseMinerProject purchaseMinerProject : purchaseMinerProjectList) {
            // 如果剩余奖励为0，跳出循环
            if (remainingReward.compareTo(BigDecimal.ZERO) <= 0) {
                log.info("【奖励上限检查】用户{}剩余奖励为0，停止分配", walletAddress);
                break;
            }

            // 跳过已停止的矿机
            if (PurchaseMinerProjectStatus.STOP.getCode().equals(purchaseMinerProject.getStatus())) {
                log.info("【奖励上限检查】用户{}的矿机{}已停止，跳过", walletAddress, purchaseMinerProject.getId());
                continue;
            }

            processedMinerCount++;
            
            // 矿机当前已产生的总价值
            BigDecimal currentTotalValue = new BigDecimal(purchaseMinerProject.getRewardPrice()).setScale(8, RoundingMode.DOWN);
            
            // 矿机的2倍购买价格（奖励上限）
            BigDecimal maxRewardValue = new BigDecimal(purchaseMinerProject.getPrice()).multiply(new BigDecimal(2)).setScale(8, RoundingMode.DOWN);
            
            // 计算这个矿机还能接受的最大价值
            BigDecimal availableValue = maxRewardValue.subtract(currentTotalValue);
            
            log.info("【奖励上限检查】用户{}矿机{} - 当前价值: {}, 上限: {}, 可用价值: {}",
                walletAddress, purchaseMinerProject.getId(), currentTotalValue, maxRewardValue, availableValue);
            
            if (availableValue.compareTo(BigDecimal.ZERO) <= 0) {
                // 矿机已经达到上限，跳过
                log.info("【奖励上限检查】用户{}的矿机{}已达到2倍奖励上限（当前价值: {}, 上限: {}），跳过奖励分配",
                    walletAddress, purchaseMinerProject.getId(), currentTotalValue, maxRewardValue);

                purchaseMinerProject.setStatus(PurchaseMinerProjectStatus.STOP.getCode());
                purchaseMinerProjectMapper.updateById(purchaseMinerProject);
                // 矿机达到上限停用，清除用户算力缓存，让下次查询时重新计算
                computingPowerServiceV2.invalidateUserCache(purchaseMinerProject.getWalletAddress());

                continue;
            }
            
            // 计算这个矿机实际能接受的奖励数量
            BigDecimal availableReward = availableValue.divide(price, 8, RoundingMode.DOWN);
            
            // 实际分配给这个矿机的奖励数量（取较小值）
            BigDecimal actualRewardForThisMiner = remainingReward.min(availableReward);
            
            // 计算实际分配的价值
            BigDecimal actualValueForThisMiner = actualRewardForThisMiner.multiply(price).setScale(8, RoundingMode.DOWN);
            
            log.info("【奖励上限检查】用户{}矿机{} - 可用奖励: {}, 分配奖励: {}, 分配价值: {}",
                walletAddress, purchaseMinerProject.getId(), availableReward, actualRewardForThisMiner, actualValueForThisMiner);
            
            // 更新矿机信息
            BigDecimal totalReward = new BigDecimal(purchaseMinerProject.getReward()).add(actualRewardForThisMiner);
            BigDecimal finalTotalValue = currentTotalValue.add(actualValueForThisMiner);
            
            purchaseMinerProject.setReward(totalReward.toString());
            purchaseMinerProject.setRewardPrice(finalTotalValue.toString());
            purchaseMinerProject.setUpdateTime(System.currentTimeMillis());
            
            // 如果达到上限，停止矿机
            if (finalTotalValue.compareTo(maxRewardValue) >= 0) {
                purchaseMinerProject.setStatus(PurchaseMinerProjectStatus.STOP.getCode());
                log.info("【奖励上限检查】用户{}的矿机{}达到2倍奖励上限（最终价值: {}, 上限: {}），停止奖励发放",
                    walletAddress, purchaseMinerProject.getId(), finalTotalValue, maxRewardValue);
                
                // 更新算力统计（矿机停止，算力减少）
                // 矿机达到上限停用，清除用户算力缓存，让下次查询时重新计算
                computingPowerServiceV2.invalidateUserCache(purchaseMinerProject.getWalletAddress());
                maxedOutMinerCount++;
            }
            
            purchaseMinerProjectMapper.updateById(purchaseMinerProject);
            
            // 累加实际发放的奖励
            actualReward = actualReward.add(actualRewardForThisMiner);
            remainingReward = remainingReward.subtract(actualRewardForThisMiner);

            LambdaQueryWrapper<MinerDailyReward> minerDailyRewardLambdaQueryWrapper = new LambdaQueryWrapper<>();
            minerDailyRewardLambdaQueryWrapper.eq(MinerDailyReward::getMinerProjectId,purchaseMinerProject.getId());
            minerDailyRewardLambdaQueryWrapper.eq(MinerDailyReward::getDayTime,dayTime);

            MinerDailyReward minerDailyReward = minerDailyRewardMapper.selectOne(minerDailyRewardLambdaQueryWrapper);
            if (Objects.isNull(minerDailyReward)){
                minerDailyReward = new MinerDailyReward();
                minerDailyReward.setMinerId(purchaseMinerProject.getMinerProjectId());
                minerDailyReward.setMinerProjectId(purchaseMinerProject.getId());
                minerDailyReward.setWalletAddress(purchaseMinerProject.getWalletAddress());
                minerDailyReward.setDayTime(dayTime);
                minerDailyReward.setTotalReward("0");
                minerDailyReward.setTotalRewardPrice("0");
                minerDailyReward.setComputingPower(purchaseMinerProject.getComputingPower());
                minerDailyReward.setCreateTime(System.currentTimeMillis());
            }

            BigDecimal minerDailyTotalReward = new BigDecimal(minerDailyReward.getTotalReward()).add(actualRewardForThisMiner);

            BigDecimal minerDailyTotalRewardPrice = new BigDecimal(minerDailyReward.getTotalRewardPrice()).add(actualValueForThisMiner);

            minerDailyReward.setTotalRewardPrice(minerDailyTotalRewardPrice.toString());
            minerDailyReward.setTotalReward(minerDailyTotalReward.toString());
            minerDailyReward.setUpdateTime(System.currentTimeMillis());
            if (Objects.isNull(minerDailyReward.getId())){
                minerDailyRewardMapper.insert(minerDailyReward);
            }else {
                minerDailyRewardMapper.updateById(minerDailyReward);
            }
        }

        if (remainingReward.compareTo(BigDecimal.ZERO) > 0) {
            log.info("【奖励上限检查】用户{}所有矿机都达到上限，舍弃剩余奖励: {}（原奖励: {}, 实际发放: {}）",
                walletAddress, remainingReward, reward, actualReward);
        }
        
        log.info("【奖励上限检查】用户{}奖励分配完成: 原奖励={}, 实际发放={}, 剩余未分配={}, 处理矿机数={}, 达到上限矿机数={}", 
            walletAddress, reward, actualReward, remainingReward, processedMinerCount, maxedOutMinerCount);
        return actualReward;
    }

    /**
     * 记录奖励发放日志
     * @param walletAddress 钱包地址
     * @param rewardType 奖励类型
     * @param dynamicRewardType 动态奖励子类型
     * @param calculatedReward 计算奖励
     * @param actualReward 实际奖励
     * @param discardedReward 舍去奖励
     * @param discardReason 舍去原因
     * @param minerId 矿机ID
     * @param computingPower 算力
     * @param totalComputingPower 总算力
     * @param order 订单号
     * @param dayTime 日期
     */
    private void recordRewardLog(String walletAddress, String rewardType, String dynamicRewardType,
                                 BigDecimal calculatedReward, BigDecimal actualReward, BigDecimal discardedReward,
                                 String discardReason, Integer minerId, String computingPower, 
                                 String totalComputingPower, String order, String dayTime) {
        try {
            RewardLog rewardLog = RewardLog.builder()
                    .walletAddress(walletAddress)
                    .rewardType(rewardType)
                    .dynamicRewardType(dynamicRewardType)
                    .calculatedReward(calculatedReward.toString())
                    .actualReward(actualReward.toString())
                    .discardedReward(discardedReward.toString())
                    .discardReason(discardReason)
                    .minerId(minerId)
                    .computingPower(computingPower)
                    .totalComputingPower(totalComputingPower)
                    .order(order)
                    .dayTime(dayTime)
                    .createTime(System.currentTimeMillis())
                    .updateTime(System.currentTimeMillis())
                    .build();
            
            rewardLogMapper.insert(rewardLog);
        } catch (Exception e) {
            log.error("记录奖励日志失败: {}", e.getMessage(), e);
        }
    }

    /**
     *  扣除发放奖励服务费
     * @param dayTime
     * @param rewardList
     * @return
     */
    public SingleResponse<Void> addRewardServiceLog(String dayTime, List<PurchaseMinerProjectReward> rewardList){

        LambdaQueryWrapper<SystemConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemConfig::getName, SystemConfigEnum.REWARD_SERVICE.getCode());

        SystemConfig systemConfig = systemConfigMapper.selectOne(queryWrapper);
        if (Objects.isNull(systemConfig)){
            return SingleResponse.buildSuccess();
        }

        Map<String, List<PurchaseMinerProjectReward>> walletAddressRewardMap = rewardList.stream().collect(Collectors.groupingBy(PurchaseMinerProjectReward::getWalletAddress));

        for (Map.Entry<String, List<PurchaseMinerProjectReward>> entry : walletAddressRewardMap.entrySet()){

            String order = "RS" + System.currentTimeMillis();

            BigDecimal totalReward = entry.getValue()
                    .stream()
                    .map(PurchaseMinerProjectReward::getReward)
                    .map(BigDecimal::new)
                    .reduce(BigDecimal::add)
                    .orElse(BigDecimal.ZERO);

            RewardServiceLog rewardServiceLog  = new RewardServiceLog();
            rewardServiceLog.setReward(totalReward.toString());
            rewardServiceLog.setWalletAddress(entry.getKey());
            rewardServiceLog.setDayTime(dayTime);
            rewardServiceLog.setOrder(order);

            BigDecimal rewardService = totalReward.multiply(new BigDecimal(systemConfig.getValue()));

            LambdaQueryWrapper<Account> accountLambdaQueryWrapper = new LambdaQueryWrapper<>();
            accountLambdaQueryWrapper.eq(Account::getWalletAddress,entry.getKey());
            accountLambdaQueryWrapper.eq(Account::getType,AccountType.ESG.getCode());

            Account account = accountMapper.selectOne(accountLambdaQueryWrapper);

            if (Objects.isNull(account)){
                log.info("钱包地址:{} 没有ESG账户",entry.getKey());
                rewardServiceLog.setStatus(PurchaseMinerProjectStatus.FAIL.getCode());
                rewardServiceLog.setReason("没有ESG账户");
                rewardServiceLogMapper.insert(rewardServiceLog);
                continue;
            }

            BigDecimal accountEsgNumber = new BigDecimal(account.getNumber());

            BigDecimal esgNumber = BigDecimal.ZERO;

            BigDecimal ecoNumber = BigDecimal.ZERO;

            if (accountEsgNumber.compareTo(rewardService) >=0){
                esgNumber = rewardService;
            }else {
                esgNumber = accountEsgNumber;
                ecoNumber = rewardService.subtract(esgNumber);
            }

            rewardServiceLog.setEcoNumber(ecoNumber.toString());
            rewardServiceLog.setEsgNumber(esgNumber.toString());


            try {

                if (esgNumber.compareTo(BigDecimal.ZERO) > 0){

                    AccountDeductCmd accountDeductCmd = new AccountDeductCmd();
                    accountDeductCmd.setAccountType(AccountType.ESG.getCode());
                    accountDeductCmd.setOrder(order);
                    accountDeductCmd.setWalletAddress(entry.getKey());
                    accountDeductCmd.setNumber(esgNumber.toString());

                    SingleResponse<Void> response = accountService.rewardService(accountDeductCmd);
                    if (!response.isSuccess()){
                        rewardServiceLog.setStatus(PurchaseMinerProjectStatus.FAIL.getCode());
                        rewardServiceLog.setReason(response.getErrMessage());
                        rewardServiceLogMapper.insert(rewardServiceLog);

                        continue;
                    }
                }

                if (ecoNumber.compareTo(BigDecimal.ZERO) > 0){

                    AccountDeductCmd accountDeductCmd = new AccountDeductCmd();
                    accountDeductCmd.setAccountType(AccountType.ECO.getCode());
                    accountDeductCmd.setOrder(order);
                    accountDeductCmd.setWalletAddress(entry.getKey());
                    accountDeductCmd.setNumber(ecoNumber.toString());

                    SingleResponse<Void> response = accountService.rewardService(accountDeductCmd);
                    if (!response.isSuccess()){
                        rewardServiceLog.setStatus(PurchaseMinerProjectStatus.FAIL.getCode());
                        rewardServiceLog.setReason(response.getErrMessage());
                        rewardServiceLogMapper.insert(rewardServiceLog);

                        continue;
                    }
                }

                rewardServiceLog.setStatus(PurchaseMinerProjectStatus.SUCCESS.getCode());
                rewardServiceLogMapper.insert(rewardServiceLog);
            }catch (Exception e){

                rewardServiceLog.setStatus(PurchaseMinerProjectStatus.FAIL.getCode());
                rewardServiceLog.setReason("扣除服务费失败");
                rewardServiceLogMapper.insert(rewardServiceLog);
                e.printStackTrace();

            }

        }

        return SingleResponse.buildSuccess();
    }

    /**
     * 添加历史价格接口
     */
    public SingleResponse<Void> addHistoryPrice(SystemConfig ecoPrice){


        SystemConfigLog systemConfigLog = new SystemConfigLog();
        systemConfigLog.setName(ecoPrice.getName());
        systemConfigLog.setValue(ecoPrice.getValue());
        systemConfigLog.setCreateTime(System.currentTimeMillis());
        systemConfigLogMapper.insert(systemConfigLog);

        return SingleResponse.buildSuccess();
    }

}
