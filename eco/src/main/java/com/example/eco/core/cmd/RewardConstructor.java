package com.example.eco.core.cmd;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.AccountDynamicNumberCmd;
import com.example.eco.bean.cmd.AccountStaticNumberCmd;
import com.example.eco.bean.cmd.PurchaseMinerProjectRewardCmd;
import com.example.eco.bean.cmd.RecommendStatisticsLogListQry;
import com.example.eco.bean.dto.RecommendStatisticsLogDTO;
import com.example.eco.common.*;
import com.example.eco.core.service.AccountService;
import com.example.eco.core.service.RecommendStatisticsLogService;
import com.example.eco.model.entity.*;
import com.example.eco.model.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RewardConstructor {

    @Resource
    private PurchaseMinerProjectMapper purchaseMinerProjectMapper;
    @Resource
    private MinerProjectMapper minerProjectMapper;
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


    /**
     * 发放算力奖励
     */
    public SingleResponse<Void> reward(PurchaseMinerProjectRewardCmd purchaseMinerProjectRewardCmd) {

        // 获取推荐统计信息
        RecommendStatisticsLogListQry recommendStatisticsLogListQry = new RecommendStatisticsLogListQry();

        MultiResponse<RecommendStatisticsLogDTO> response = recommendStatisticsLogService.list(recommendStatisticsLogListQry);
        if (!response.isSuccess()) {
            return SingleResponse.buildFailure(response.getErrMessage());
        }

        // 计算总算力
        BigDecimal totalComputingPower = response.getData()
                .stream()
                .map(RecommendStatisticsLogDTO::getTotalComputingPower)
                .map(BigDecimal::new)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        if (totalComputingPower.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("总算力为0，无需发放奖励");
            return SingleResponse.buildSuccess();
        }

        // 获取矿机算力要求配置
        SingleResponse<BigDecimal> minerRequirementResponse = getMinerRequirement(totalComputingPower);
        if (!minerRequirementResponse.isSuccess()) {
            return SingleResponse.buildFailure(minerRequirementResponse.getErrMessage());
        }

        BigDecimal minerRequirement = minerRequirementResponse.getData();


        // 获取每天总奖励数
        SingleResponse<BigDecimal> totalRewardResponse = getTotalReward(totalComputingPower, minerRequirement);
        if (!totalRewardResponse.isSuccess()) {
            return SingleResponse.buildFailure(totalRewardResponse.getErrMessage());
        }
        BigDecimal totalReward = totalRewardResponse.getData();

        log.info("总算力: {}, 矿机算力要求: {}, 每天总奖励数: {}", totalComputingPower, minerRequirement, totalReward);
        if (totalReward.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("每天总奖励数为0，无需发放奖励");
            return SingleResponse.buildSuccess();
        }

        // 计算静态奖励
        staticReward(totalComputingPower, totalReward, purchaseMinerProjectRewardCmd.getDayTime());

        // 计算动态奖励
        dynamicReward(purchaseMinerProjectRewardCmd.getDayTime(), totalReward, totalComputingPower, response.getData());

        return SingleResponse.buildSuccess();
    }

    /**
     * 检查用户是否有挖矿资格
     */
    public SingleResponse<Recommend> checkRecommend(String walletAddress) {

        LambdaQueryWrapper<Recommend> recommendQueryWrapper = new LambdaQueryWrapper<>();
        recommendQueryWrapper.eq(Recommend::getWalletAddress, walletAddress);

        Recommend recommend = recommendMapper.selectOne(recommendQueryWrapper);
        if (recommend == null) {
            log.info("用户{}未绑定推荐人，无法发放静态奖励", walletAddress);
            return SingleResponse.buildFailure("用户未绑定推荐人");
        }

        if (recommend.getStatus().equals(RecommendStatus.STOP.getCode())) {
            log.info("用户{}推荐人状态为{}，无法发放静态奖励", walletAddress, RecommendStatus.STOP.getName());
            return SingleResponse.buildFailure("推荐人状态异常");
        }

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
        // 计算出多余的算力
        BigDecimal moreComputingPower = totalComputingPower.subtract(minerRequirement);

        // 计算出新增矿机数量
        if (moreComputingPower.compareTo(minerAddNumberRequirement) < 0) {
            log.info("新增算力{}未达到新增矿机挖矿数量要求{}，不增加挖矿数量", moreComputingPower, minerAddNumberRequirement);
        } else {
            // 新增挖矿数量的倍数 = 新增算力 / 新增矿机挖矿数量要求 （取余）
            BigDecimal times = moreComputingPower.divide(minerAddNumberRequirement, 0, RoundingMode.HALF_DOWN);

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
        if (staticRewardRate.compareTo(BigDecimal.ZERO) < 0 || staticRewardRate.compareTo(BigDecimal.ONE) > 0) {
            return SingleResponse.buildFailure("静态奖励比例系统配置错误");
        }
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
        if (dynamicRewardRate.compareTo(BigDecimal.ZERO) < 0 || dynamicRewardRate.compareTo(BigDecimal.ONE) > 0) {
            return SingleResponse.buildFailure("动态奖励比例系统配置错误");
        }
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
        if (dynamicRewardRecommendRate.compareTo(BigDecimal.ZERO) < 0 || dynamicRewardRecommendRate.compareTo(BigDecimal.ONE) > 0) {
            return SingleResponse.buildFailure("动态奖励推荐人比例系统配置错误");
        }
        return SingleResponse.of(dynamicRewardRecommendRate);
    }


    public SingleResponse<Void> staticReward(BigDecimal totalComputingPower, BigDecimal totalReward, String dayTime) {

        LambdaQueryWrapper<PurchaseMinerProject> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        List<PurchaseMinerProject> purchaseMinerProjectList = purchaseMinerProjectMapper.selectList(lambdaQueryWrapper);

        // 获取静态奖励比例
        SingleResponse<BigDecimal> staticRewardRateResponse = getStaticRewardRate();
        if (!staticRewardRateResponse.isSuccess()) {
            return SingleResponse.buildFailure(staticRewardRateResponse.getErrMessage());
        }

        BigDecimal staticRewardRate = staticRewardRateResponse.getData();

        BigDecimal staticTotalReward = totalReward.multiply(staticRewardRate);

        // 遍历所有矿机，计算静态奖励
        for (PurchaseMinerProject purchaseMinerProject : purchaseMinerProjectList) {

            // 检查用户是否有挖矿资格
            SingleResponse<Recommend> recommendResponse = checkRecommend(purchaseMinerProject.getWalletAddress());
            if (!recommendResponse.isSuccess()) {
                continue;
            }

            Recommend recommend = recommendResponse.getData();

            // 获取每个矿机的静态奖励
            staticReward(purchaseMinerProject, recommend, totalComputingPower, staticTotalReward, dayTime);

        }

        return SingleResponse.buildSuccess();

    }

    /**
     * 获取每个矿机的静态奖励
     */
    public PurchaseMinerProjectReward staticReward(PurchaseMinerProject purchaseMinerProject,
                                                   Recommend recommend,
                                                   BigDecimal totalComputingPower,
                                                   BigDecimal staticTotalReward,
                                                   String dayTime) {
        // 计算静态奖励 = 每天总奖励数 * 静态奖励比例 * (用户矿机算力 / 总算力)


        String order = "ST" + System.currentTimeMillis();

        BigDecimal computingPower = new BigDecimal(purchaseMinerProject.getComputingPower());

        BigDecimal staticReward = computingPower.divide(totalComputingPower, 8, RoundingMode.HALF_DOWN).multiply(staticTotalReward);


        PurchaseMinerProjectReward purchaseMinerProjectReward = new PurchaseMinerProjectReward();
        purchaseMinerProjectReward.setOrder(order);
        purchaseMinerProjectReward.setPurchaseMinerProjectId(purchaseMinerProject.getId());
        purchaseMinerProjectReward.setReward(staticReward.toString());
        purchaseMinerProjectReward.setType(PurchaseMinerProjectRewardType.STATIC.getCode());
        purchaseMinerProjectReward.setWalletAddress(purchaseMinerProject.getWalletAddress());
        purchaseMinerProjectReward.setLeaderWalletAddress(recommend.getLeaderWalletAddress());
        purchaseMinerProjectReward.setComputingPower(purchaseMinerProject.getComputingPower());
        purchaseMinerProjectReward.setTotalComputingPower(totalComputingPower.toString());
        purchaseMinerProjectReward.setRecommendWalletAddress(recommend.getRecommendWalletAddress());
        purchaseMinerProjectReward.setDayTime(dayTime);
        purchaseMinerProjectReward.setCreateTime(System.currentTimeMillis());
        int insert = purchaseMinerProjectRewardMapper.insert(purchaseMinerProjectReward);

        if (insert <= 0) {
            log.info("用户{}发放静态奖励失败", purchaseMinerProject.getWalletAddress());
            return null;
        }

        log.info("用户{}发放静态奖励{}成功", purchaseMinerProject.getWalletAddress(), staticReward);


        try {
            AccountStaticNumberCmd accountStaticNumberCmd = new AccountStaticNumberCmd();
            accountStaticNumberCmd.setWalletAddress(purchaseMinerProject.getWalletAddress());
            accountStaticNumberCmd.setNumber(staticReward.toString());
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
     * 动态奖励
     */
    public SingleResponse<Void> dynamicReward(String dayTime,
                                              BigDecimal totalReward,
                                              BigDecimal totalComputingPower,
                                              List<RecommendStatisticsLogDTO> recommendStatisticsLogList) {

        // 获取动态奖励比例
        SingleResponse<BigDecimal> dynamicRewardRateResponse = getDynamicRewardRate();
        if (!dynamicRewardRateResponse.isSuccess()) {
            return SingleResponse.buildFailure(dynamicRewardRateResponse.getErrMessage());
        }

        BigDecimal dynamicRewardRate = dynamicRewardRateResponse.getData();

        // 计算动态奖励总额
        BigDecimal dynamicTotalReward = totalReward.multiply(dynamicRewardRate);


        // 动态推荐奖励
        SingleResponse<Void> recommendRewardResponse = recommendReward(dynamicTotalReward, recommendStatisticsLogList, dayTime);
        if (!recommendRewardResponse.isSuccess()) {
            return SingleResponse.buildFailure(recommendRewardResponse.getErrMessage());
        }

        // 动态基础奖励
        SingleResponse<Void> baseRewardResponse = baseReward(dynamicTotalReward, recommendStatisticsLogList, dayTime);
        if (!baseRewardResponse.isSuccess()) {
            return SingleResponse.buildFailure(baseRewardResponse.getErrMessage());
        }

        // 其他动态奖励 TODO

        return SingleResponse.buildSuccess();

    }

    /**
     * 动态推荐奖励
     */
    public SingleResponse<Void> recommendReward(BigDecimal dynamicTotalReward, List<RecommendStatisticsLogDTO> recommendStatisticsLogList, String dayTime) {


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
        BigDecimal totalDirectRecommendComputingPower = recommendStatisticsLogList
                .stream()
                .map(RecommendStatisticsLogDTO::getTotalDirectRecommendComputingPower)
                .map(BigDecimal::new)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);


        for (RecommendStatisticsLogDTO totalRecommendStatisticsLog : recommendStatisticsLogList) {

            if (new BigDecimal(totalRecommendStatisticsLog.getTotalDirectRecommendComputingPower()).compareTo(BigDecimal.ZERO) <= 0) {
                // 直推算力为0，跳过
                log.info("用户{}直推算力为0，跳过", totalRecommendStatisticsLog.getWalletAddress());
                continue;
            }

            // 检查用户是否有挖矿资格
            SingleResponse<Recommend> recommendResponse = checkRecommend(totalRecommendStatisticsLog.getWalletAddress());
            if (!recommendResponse.isSuccess()) {
                continue;
            }

            Recommend recommend = recommendResponse.getData();

            // 计算动态推荐奖励
            recommendReward(totalRecommendStatisticsLog, dynamicRewardRecommendTotalReward, totalDirectRecommendComputingPower, recommend, dayTime);

        }
        return SingleResponse.buildSuccess();

    }


    /**
     * 动态推荐奖励
     */
    public SingleResponse<Void> recommendReward(RecommendStatisticsLogDTO recommendStatisticsLogDTO,
                                                BigDecimal dynamicRewardRecommendTotalReward,
                                                BigDecimal totalDirectRecommendComputingPower,
                                                Recommend recommend,
                                                String dayTime) {

        // 计算动态推荐奖励 = 推荐总奖励 * (用户直推算力 / 总直推算力)

        String order = "DTR" + System.currentTimeMillis();
        //直推总算力
        BigDecimal directRecommendComputingPower = new BigDecimal(recommendStatisticsLogDTO.getTotalDirectRecommendComputingPower());

        BigDecimal recommendReward = directRecommendComputingPower.divide(totalDirectRecommendComputingPower, 8, RoundingMode.HALF_DOWN).multiply(dynamicRewardRecommendTotalReward);

        PurchaseMinerProjectReward purchaseMinerProjectReward = new PurchaseMinerProjectReward();
        purchaseMinerProjectReward.setOrder(order);
        purchaseMinerProjectReward.setReward(recommendReward.toString());
        purchaseMinerProjectReward.setType(PurchaseMinerProjectRewardType.DYNAMIC.getCode());
        purchaseMinerProjectReward.setWalletAddress(recommendStatisticsLogDTO.getWalletAddress());
        purchaseMinerProjectReward.setLeaderWalletAddress(recommend.getLeaderWalletAddress());
        purchaseMinerProjectReward.setRecommendWalletAddress(recommend.getRecommendWalletAddress());

        purchaseMinerProjectReward.setTotalComputingPower(totalDirectRecommendComputingPower.toString());
        purchaseMinerProjectReward.setComputingPower(directRecommendComputingPower.toString());

        purchaseMinerProjectReward.setRewardType(PurchaseMinerProjectDynamicRewardType.RECOMMEND.getCode());
        purchaseMinerProjectReward.setDayTime(dayTime);
        purchaseMinerProjectReward.setCreateTime(System.currentTimeMillis());
        int insert = purchaseMinerProjectRewardMapper.insert(purchaseMinerProjectReward);
        if (insert <= 0) {
            log.info("用户{}发放动态推荐奖励失败", recommendStatisticsLogDTO.getWalletAddress());
            return SingleResponse.buildFailure("发放动态推荐奖励失败");
        }
        log.info("用户{}发放动态推荐奖励{}成功", recommendStatisticsLogDTO.getWalletAddress(), recommendReward);

        try {
            AccountDynamicNumberCmd accountDynamicNumberCmd = new AccountDynamicNumberCmd();
            accountDynamicNumberCmd.setWalletAddress(recommendStatisticsLogDTO.getWalletAddress());
            accountDynamicNumberCmd.setNumber(recommendReward.toString());
            accountDynamicNumberCmd.setType(AccountType.ECO.getCode());
            accountDynamicNumberCmd.setOrder(order);

            SingleResponse<Void> response = accountService.addDynamicNumber(accountDynamicNumberCmd);
            if (!response.isSuccess()) {
                log.info("用户{}发放动态推荐奖励{}失败，调用账户服务失败", recommendStatisticsLogDTO.getWalletAddress(), recommendReward);
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
        if (dynamicRewardBaseRate.compareTo(BigDecimal.ZERO) < 0 || dynamicRewardBaseRate.compareTo(BigDecimal.ONE) > 0) {
            return SingleResponse.buildFailure("动态奖励小区奖励比例系统配置错误");
        }
        return SingleResponse.of(dynamicRewardBaseRate);
    }


    /**
     * 动态小区奖励
     */
    public SingleResponse<Void> baseReward(BigDecimal dynamicTotalReward,
                                           List<RecommendStatisticsLogDTO> recommendStatisticsLogList,
                                           String dayTime) {

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
        BigDecimal totalMinComputingPower = recommendStatisticsLogList.stream()
                .map(RecommendStatisticsLogDTO::getMinComputingPower)
                .map(BigDecimal::new)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (RecommendStatisticsLogDTO recommendStatisticsLog : recommendStatisticsLogList) {

            // 检查用户是否有挖矿资格
            SingleResponse<Recommend> recommendResponse = checkRecommend(recommendStatisticsLog.getWalletAddress());
            if (!recommendResponse.isSuccess()) {
                continue;
            }

            Recommend recommend = recommendResponse.getData();

            // 计算动态小区奖励
            baseReward(recommendStatisticsLog, dynamicBaseTotalReward, totalMinComputingPower, recommend, dayTime);

        }


        return SingleResponse.buildSuccess();
    }


    public SingleResponse<Void> baseReward(RecommendStatisticsLogDTO recommendStatisticsLogDTO,
                                           BigDecimal dynamicBaseTotalReward,
                                           BigDecimal totalMinComputingPower,
                                           Recommend recommend,
                                           String dayTime) {

        // 计算动态小区奖励 = 动态小区奖励总额 * (用户小区总算力 / 总算力)

        String order = "DTB" + System.currentTimeMillis();

        BigDecimal minComputingPower = new BigDecimal(recommendStatisticsLogDTO.getMinComputingPower());
        // 动态奖励
        BigDecimal baseReward = minComputingPower
                .divide(totalMinComputingPower, 8, RoundingMode.HALF_DOWN)
                .multiply(dynamicBaseTotalReward);

        PurchaseMinerProjectReward purchaseMinerProjectReward = new PurchaseMinerProjectReward();
        purchaseMinerProjectReward.setOrder(order);
        purchaseMinerProjectReward.setRewardType(PurchaseMinerProjectDynamicRewardType.BASE.getCode());
        purchaseMinerProjectReward.setReward(baseReward.toString());
        purchaseMinerProjectReward.setType(PurchaseMinerProjectRewardType.DYNAMIC.getCode());
        purchaseMinerProjectReward.setDayTime(dayTime);
        purchaseMinerProjectReward.setComputingPower(recommendStatisticsLogDTO.getTotalComputingPower());
        purchaseMinerProjectReward.setTotalComputingPower(totalMinComputingPower.toString());
        purchaseMinerProjectReward.setMinComputingPower(recommendStatisticsLogDTO.getMinComputingPower());
        purchaseMinerProjectReward.setMaxComputingPower(recommendStatisticsLogDTO.getMaxComputingPower());
        purchaseMinerProjectReward.setWalletAddress(recommend.getWalletAddress());
        purchaseMinerProjectReward.setRecommendWalletAddress(recommend.getRecommendWalletAddress());
        purchaseMinerProjectReward.setLeaderWalletAddress(recommend.getLeaderWalletAddress());
        purchaseMinerProjectReward.setCreateTime(System.currentTimeMillis());

        int insert = purchaseMinerProjectRewardMapper.insert(purchaseMinerProjectReward);
        if (insert <= 0) {
            log.info("用户{}发放动态小区奖励失败", recommendStatisticsLogDTO.getWalletAddress());
            return SingleResponse.buildFailure("发放动态小区奖励失败");
        }
        log.info("用户{}发放动态小区奖励失败{}成功", recommendStatisticsLogDTO.getWalletAddress(), baseReward);

        try {
            AccountDynamicNumberCmd accountDynamicNumberCmd = new AccountDynamicNumberCmd();
            accountDynamicNumberCmd.setWalletAddress(recommendStatisticsLogDTO.getWalletAddress());
            accountDynamicNumberCmd.setNumber(baseReward.toString());
            accountDynamicNumberCmd.setType(AccountType.ECO.getCode());
            accountDynamicNumberCmd.setOrder(order);

            SingleResponse<Void> response = accountService.addDynamicNumber(accountDynamicNumberCmd);
            if (!response.isSuccess()) {
                log.info("用户{}发放动态小区奖励{}失败，调用账户服务失败", recommendStatisticsLogDTO.getWalletAddress(), baseReward);
            }

        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return SingleResponse.buildSuccess();
    }
}
