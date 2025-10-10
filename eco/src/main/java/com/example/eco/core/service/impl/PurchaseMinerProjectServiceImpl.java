package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.bean.dto.PurchaseMinerProjectDTO;
import com.example.eco.bean.dto.PurchaseMinerProjectRewardDTO;
import com.example.eco.bean.dto.PurchaseMinerProjectStatisticsDTO;
import com.example.eco.bean.dto.RewardServiceResultDTO;
import com.example.eco.common.*;
import com.example.eco.common.BusinessException;
import com.example.eco.core.service.AccountService;
import com.example.eco.core.service.MinerProjectService;
import com.example.eco.core.service.PurchaseMinerProjectService;
import com.example.eco.core.service.RecommendStatisticsLogService;
import com.example.eco.core.service.impl.ComputingPowerServiceImplV2;
import com.example.eco.model.entity.*;
import com.example.eco.model.mapper.*;
import com.example.eco.util.ESGUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PurchaseMinerProjectServiceImpl implements PurchaseMinerProjectService {

    @Resource
    private PurchaseMinerProjectMapper purchaseMinerProjectMapper;
    @Resource
    private MinerProjectMapper minerProjectMapper;
    @Resource
    private AccountService accountService;
    @Resource
    private RecommendStatisticsLogService recommendStatisticsLogService;
    @Resource
    private ComputingPowerServiceImplV2 computingPowerServiceV2;
    @Resource
    private MinerProjectService minerProjectService;
    @Resource
    private MinerProjectStatisticsLogMapper minerProjectStatisticsLogMapper;
    @Resource
    private SystemConfigMapper systemConfigMapper;
    @Resource
    private ESGUtils esgUtils;
    @Resource
    private RewardStatisticsLogMapper rewardStatisticsLogMapper;
    @Resource
    private PurchaseMinerProjectRewardMapper purchaseMinerProjectRewardMapper;
    @Resource
    private MinerDailyRewardMapper minerDailyRewardMapper;
    @Resource
    private RewardServiceLogMapper rewardServiceLogMapper;


    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> create(PurchaseMinerProjectsCreateCmd purchaseMinerProjectsCreateCmd) {

        MinerProject minerProject = minerProjectMapper.selectById(purchaseMinerProjectsCreateCmd.getMinerProjectId());
        if (minerProject == null) {
            return SingleResponse.buildFailure("矿机不存在");
        }

        String dayTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        if (!purchaseMinerProjectsCreateCmd.getType().equals(PurchaseMinerType.ECO.getCode()) &&
                !purchaseMinerProjectsCreateCmd.getType().equals(PurchaseMinerType.AIRDROP.getCode()) ) {
            Boolean checkQuota = checkQuota(minerProject, dayTime);
            if (!checkQuota) {
                return SingleResponse.buildFailure("该矿机ESG已达限额,请使用ECO支付");
            }
        }

        // todo U -> 转ESG ECO

        String order = "PMP" + System.currentTimeMillis();

        PurchaseMinerProject purchaseMinerProject = new PurchaseMinerProject();
        purchaseMinerProject.setMinerProjectId(purchaseMinerProjectsCreateCmd.getMinerProjectId());
        purchaseMinerProject.setPrice(minerProject.getPrice());
        purchaseMinerProject.setComputingPower(minerProject.getComputingPower());
        purchaseMinerProject.setActualComputingPower(minerProject.getComputingPower());
        purchaseMinerProject.setType(purchaseMinerProjectsCreateCmd.getType());
        purchaseMinerProject.setWalletAddress(purchaseMinerProjectsCreateCmd.getWalletAddress());
        purchaseMinerProject.setStatus(PurchaseMinerProjectStatus.DEALING.getCode());
        purchaseMinerProject.setOrder(order);
        purchaseMinerProject.setReward("0");
        purchaseMinerProject.setRewardPrice("0");
        purchaseMinerProject.setCreateTime(System.currentTimeMillis());

        // 计算动态补偿算力 - 从数据库获取倍数配置，使用1.003^(n-1)公式
        BigDecimal actualComputingPower = calculateDynamicCompensationPower(minerProject, purchaseMinerProject.getCreateTime());
        
        if (actualComputingPower.compareTo(new BigDecimal(minerProject.getComputingPower())) > 0) {
            // 设置补偿算力
            purchaseMinerProject.setActualComputingPower(actualComputingPower.toString());
            
            // 设置加速到期时间（从购买时开始，有效期从配置获取）
            Long effectiveTime = calculateEffectiveTime();
            purchaseMinerProject.setAccelerateExpireTime(effectiveTime);
        } else {
            // 如果没有补偿，使用原始算力
            purchaseMinerProject.setActualComputingPower(minerProject.getComputingPower());
        }


        BigDecimal amount = BigDecimal.ZERO;

        BigDecimal totalEsgNumber = BigDecimal.ZERO;

        if (purchaseMinerProjectsCreateCmd.getType().equals(PurchaseMinerType.ESG.getCode())) {

            BigDecimal esgPrice = esgUtils.getEsgPrice();

            if (esgPrice.compareTo(BigDecimal.ZERO) <= 0) {
                return SingleResponse.buildFailure("获取ESG价格失败");
            }

            BigDecimal esgNumber = new BigDecimal(minerProject.getPrice()).divide(esgPrice, 4, RoundingMode.HALF_DOWN);
            purchaseMinerProject.setEsgNumber(esgNumber.toString());

            AccountDeductCmd accountDeductCmd = new AccountDeductCmd();
            accountDeductCmd.setAccountType(AccountType.ESG.getCode());
            accountDeductCmd.setNumber(esgNumber.toString());
            accountDeductCmd.setWalletAddress(purchaseMinerProjectsCreateCmd.getWalletAddress());
            accountDeductCmd.setOrder(order);

            try {

                SingleResponse<Void> response = accountService.purchaseMinerProjectNumber(accountDeductCmd);
                if (!response.isSuccess()) {
                    return response;
                } else {
                    // 购买成功，记录购买信息
                    purchaseMinerProject.setStatus(PurchaseMinerProjectStatus.SUCCESS.getCode());
                    purchaseMinerProject.setFinishTime(System.currentTimeMillis());

                    amount = amount.add(new BigDecimal(minerProject.getPrice()));
                    totalEsgNumber = totalEsgNumber.add(esgNumber);

                }
            }catch (Exception e){
                e.printStackTrace();
                throw new RuntimeException("购买矿机异常");
            }
        }

        if (purchaseMinerProjectsCreateCmd.getType().equals(PurchaseMinerType.ECO.getCode())) {

            LambdaQueryWrapper<SystemConfig> systemConfigLambdaQueryWrapper = new LambdaQueryWrapper<>();
            systemConfigLambdaQueryWrapper.eq(SystemConfig::getName, SystemConfigEnum.ECO_PRICE.getCode());

            SystemConfig systemConfig = systemConfigMapper.selectOne(systemConfigLambdaQueryWrapper);
            if (Objects.isNull(systemConfig)) {
                return SingleResponse.buildFailure("未设置ECO价格");
            }

            BigDecimal ecoNumber = new BigDecimal(minerProject.getPrice()).divide(new BigDecimal(systemConfig.getValue()), 4, RoundingMode.HALF_DOWN);
            purchaseMinerProject.setEcoNumber(ecoNumber.toString());

            AccountDeductCmd accountDeductCmd = new AccountDeductCmd();
            accountDeductCmd.setAccountType(AccountType.ECO.getCode());
            accountDeductCmd.setNumber(ecoNumber.toString());
            accountDeductCmd.setWalletAddress(purchaseMinerProjectsCreateCmd.getWalletAddress());
            accountDeductCmd.setOrder(order);

            try {

                SingleResponse<Void> response = accountService.purchaseMinerProjectNumber(accountDeductCmd);
                if (!response.isSuccess()) {

                    return response;
                } else {
                    // 购买成功，记录购买信息
                    purchaseMinerProject.setStatus(PurchaseMinerProjectStatus.SUCCESS.getCode());
                    purchaseMinerProject.setFinishTime(System.currentTimeMillis());
                }
            }catch (Exception e){
                e.printStackTrace();
                throw new RuntimeException("购买矿机异常");
            }

        }

        if (purchaseMinerProjectsCreateCmd.getType().equals(PurchaseMinerType.ECO_ESG.getCode())) {

            BigDecimal esgPrice = esgUtils.getEsgPrice();

            if (esgPrice.compareTo(BigDecimal.ZERO) <= 0) {
                return SingleResponse.buildFailure("获取ESG价格失败");
            }

            LambdaQueryWrapper<SystemConfig> systemConfigLambdaQueryWrapper = new LambdaQueryWrapper<>();
            systemConfigLambdaQueryWrapper.eq(SystemConfig::getName, SystemConfigEnum.ECO_PRICE.getCode());
            SystemConfig systemConfig = systemConfigMapper.selectOne(systemConfigLambdaQueryWrapper);
            if (Objects.isNull(systemConfig)) {
                return SingleResponse.buildFailure("未设置ECO价格");
            }

//            BigDecimal halfPrice = new BigDecimal(minerProject.getPrice()).divide(new BigDecimal(2), 4, RoundingMode.HALF_DOWN);


            BigDecimal ecoNumber = new BigDecimal(minerProject.getPrice()).divide(new BigDecimal(systemConfig.getValue()), 4, RoundingMode.HALF_DOWN);

            BigDecimal esgNumber = ecoNumber.divide(BigDecimal.valueOf(2));

            ecoNumber = esgNumber;

            amount = amount.add(esgNumber.multiply(esgPrice));

            purchaseMinerProject.setEsgNumber(esgNumber.toString());
            purchaseMinerProject.setEcoNumber(ecoNumber.toString());

            AccountDeductCmd ecoAccountDeductCmd = new AccountDeductCmd();
            ecoAccountDeductCmd.setAccountType(AccountType.ECO.getCode());
            ecoAccountDeductCmd.setNumber(ecoNumber.toString());
            ecoAccountDeductCmd.setWalletAddress(purchaseMinerProjectsCreateCmd.getWalletAddress());
            ecoAccountDeductCmd.setOrder(order);

            try {

                SingleResponse<Void> ecoResponse = accountService.purchaseMinerProjectNumber(ecoAccountDeductCmd);
                if (!ecoResponse.isSuccess()) {
                    return ecoResponse;
                }
            }catch (Exception e){
                e.printStackTrace();
                throw new RuntimeException("购买矿机异常");
            }


            AccountDeductCmd esgAccountDeductCmd = new AccountDeductCmd();
            esgAccountDeductCmd.setAccountType(AccountType.ESG.getCode());
            esgAccountDeductCmd.setNumber(esgNumber.toString());
            esgAccountDeductCmd.setWalletAddress(purchaseMinerProjectsCreateCmd.getWalletAddress());
            esgAccountDeductCmd.setOrder(order);

            try {
                SingleResponse<Void> esgResponse = accountService.purchaseMinerProjectNumber(esgAccountDeductCmd);
                if (!esgResponse.isSuccess()) {
                    throw new BusinessException("ESG支付失败: " + esgResponse.getErrMessage());
                }
            }catch (Exception e){
                e.printStackTrace();
                throw new BusinessException(e.getMessage());
            }


            // 购买成功，记录购买信息
            purchaseMinerProject.setStatus(PurchaseMinerProjectStatus.SUCCESS.getCode());
            purchaseMinerProject.setFinishTime(System.currentTimeMillis());

            totalEsgNumber = totalEsgNumber.add(esgNumber);
        }

        if (purchaseMinerProjectsCreateCmd.getType().equals(PurchaseMinerType.AIRDROP.getCode())){
            purchaseMinerProject.setEsgNumber("0");
            purchaseMinerProject.setEcoNumber("0");
            purchaseMinerProject.setStatus(PurchaseMinerProjectStatus.SUCCESS.getCode());
            purchaseMinerProject.setFinishTime(System.currentTimeMillis());
        }

        try {

            // 矿机购买成功，清除用户算力缓存，让下次查询时重新计算
            computingPowerServiceV2.invalidateUserCache(purchaseMinerProject.getWalletAddress());

            if (amount.compareTo(BigDecimal.ZERO) > 0) {

                MinerProjectStatisticsLogCmd minerProjectStatisticsLogCmd = new MinerProjectStatisticsLogCmd();
                minerProjectStatisticsLogCmd.setAmount(amount);
                minerProjectStatisticsLogCmd.setMinerProjectId(minerProject.getId());
                minerProjectStatisticsLogCmd.setDayTime(dayTime);
                minerProjectStatisticsLogCmd.setEsgNumber(totalEsgNumber);

                minerProjectService.statistics(minerProjectStatisticsLogCmd);
            }
            purchaseMinerProjectMapper.insert(purchaseMinerProject);

        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("购买矿机异常");
        }

        return SingleResponse.buildSuccess();
    }


    /**
     * 计算动态补偿算力
     * 补偿算力 = 原始算力 * baseMultiplier^(n-1)
     * 其中 n = 矿机创建时间到购买时间的天数
     * baseMultiplier = 从数据库配置 INCREASE_MULTIPLIER 获取
     * 从购买时开始补偿，即 n >= 1 时就有补偿
     */
    private BigDecimal calculateDynamicCompensationPower(MinerProject minerProject, Long purchaseTime) {
        try {
            
            // 从数据库获取倍数配置
            LambdaQueryWrapper<SystemConfig> increaseQueryWrapper = new LambdaQueryWrapper<>();
            increaseQueryWrapper.eq(SystemConfig::getName, SystemConfigEnum.INCREASE_MULTIPLIER.getCode());
            SystemConfig increaseSystemConfig = systemConfigMapper.selectOne(increaseQueryWrapper);
            
            if (increaseSystemConfig == null || increaseSystemConfig.getValue() == null) {
                // 如果没有配置，返回原始算力
                return new BigDecimal(minerProject.getComputingPower());
            }


            LambdaQueryWrapper<SystemConfig> increaseStartTimeQueryWrapper = new LambdaQueryWrapper<>();
            increaseStartTimeQueryWrapper.eq(SystemConfig::getName, SystemConfigEnum.INCREASE_MULTIPLIER_START_TIME.getCode());
            SystemConfig increaseStartTimeSystemConfig = systemConfigMapper.selectOne(increaseQueryWrapper);

            if (increaseStartTimeSystemConfig == null || increaseStartTimeSystemConfig.getValue() == null) {
                // 如果没有配置，返回原始算力
                return new BigDecimal(minerProject.getComputingPower());
            }


            BigDecimal baseMultiplier = new BigDecimal(increaseSystemConfig.getValue());

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = sdf.parse(increaseStartTimeSystemConfig.getValue());
            
            // 计算天数差
            long daysDifference = calculateDaysDifference(date.getTime(), purchaseTime);
            
            // 从购买时开始补偿，即 n >= 1
            if (daysDifference < 1) {
                // 当天购买，无补偿
                return new BigDecimal(minerProject.getComputingPower());
            }
            
            // 计算补偿倍数：baseMultiplier^(n-1)
            int exponent = (int) (daysDifference - 1); // n-1
            
            // 使用BigDecimal进行幂运算
            BigDecimal compensationMultiplier = baseMultiplier.pow(exponent);
            
            // 计算补偿算力
            BigDecimal originalPower = new BigDecimal(minerProject.getComputingPower());


            return originalPower.multiply(compensationMultiplier);
            
        } catch (Exception e) {
            log.error("计算动态补偿算力失败", e);
            // 计算失败时返回原始算力
            return new BigDecimal(minerProject.getComputingPower());
        }
    }
    
    /**
     * 计算两个时间戳之间的天数差
     */
    private long calculateDaysDifference(Long startTime, Long endTime) {
        try {
            // 将时间戳转换为LocalDate
            LocalDate startDate = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(startTime), 
                ZoneId.systemDefault()
            ).toLocalDate();
            
            LocalDate endDate = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(endTime), 
                ZoneId.systemDefault()
            ).toLocalDate();
            
            // 计算天数差
            return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
            
        } catch (Exception e) {
            log.error("计算天数差失败: startTime={}, endTime={}", startTime, endTime, e);
            return 0;
        }
    }
    
    /**
     * 计算补偿算力有效期
     * 从购买时开始，有效期从数据库配置 EFFECTIVE_DAY 获取
     */
    private Long calculateEffectiveTime() {
        try {
            // 从数据库获取有效期配置
            LambdaQueryWrapper<SystemConfig> effectiveQueryWrapper = new LambdaQueryWrapper<>();
            effectiveQueryWrapper.eq(SystemConfig::getName, SystemConfigEnum.EFFECTIVE_DAY.getCode());
            SystemConfig effectiveSystemConfig = systemConfigMapper.selectOne(effectiveQueryWrapper);
            
            if (effectiveSystemConfig == null || effectiveSystemConfig.getValue() == null) {
                // 如果没有配置，默认1天有效期
                LocalDateTime effectiveDate = LocalDateTime.now().plusDays(1);
                return effectiveDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            
            // 从购买时开始，加上配置的有效天数
            long effectiveDays = Long.parseLong(effectiveSystemConfig.getValue());
            LocalDateTime effectiveDate = LocalDateTime.now().plusDays(effectiveDays);
            Long effectiveTime = effectiveDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            
            log.info("补偿算力有效期计算: 有效天数={}, 到期时间={}", effectiveDays, new java.util.Date(effectiveTime));
            
            return effectiveTime;
            
        } catch (Exception e) {
            log.error("计算补偿算力有效期失败", e);
            // 计算失败时默认1天有效期
            LocalDateTime effectiveDate = LocalDateTime.now().plusDays(1);
            return effectiveDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
    }

    /**
     * 检查限额
     */
    public Boolean checkQuota(MinerProject minerProject, String dayTime) {

        LambdaQueryWrapper<MinerProjectStatisticsLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MinerProjectStatisticsLog::getDayTime, dayTime);
        queryWrapper.eq(MinerProjectStatisticsLog::getMinerProjectId, minerProject.getId());

        MinerProjectStatisticsLog minerProjectStatisticsLog = minerProjectStatisticsLogMapper.selectOne(queryWrapper);

        if (Objects.isNull(minerProjectStatisticsLog)) {
            return Boolean.TRUE;
        }

        BigDecimal price = new BigDecimal(minerProject.getPrice());

        BigDecimal halfPrice = price.divide(new BigDecimal(minerProject.getPrice()).divide(new BigDecimal(2), 4, RoundingMode.HALF_DOWN));

        BigDecimal remaining = new BigDecimal(minerProject.getQuota()).subtract(new BigDecimal(minerProjectStatisticsLog.getAmount()));


        if (remaining.compareTo(price) < 0 || remaining.compareTo(halfPrice) < 0) {
            return Boolean.FALSE;
        } else {
            return Boolean.TRUE;
        }
    }

    @Override
    public MultiResponse<PurchaseMinerProjectDTO> page(PurchaseMinerProjectPageQry purchaseMinerProjectPageQry) {

        LambdaQueryWrapper<PurchaseMinerProject> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(StringUtils.hasLength(purchaseMinerProjectPageQry.getWalletAddress()),PurchaseMinerProject::getWalletAddress, purchaseMinerProjectPageQry.getWalletAddress());
        lambdaQueryWrapper.eq(StringUtils.hasLength(purchaseMinerProjectPageQry.getType()),PurchaseMinerProject::getType, purchaseMinerProjectPageQry.getType());
        lambdaQueryWrapper.eq(StringUtils.hasLength(purchaseMinerProjectPageQry.getStatus()),PurchaseMinerProject::getStatus, purchaseMinerProjectPageQry.getStatus());

        if (Objects.nonNull(purchaseMinerProjectPageQry.getStartTime()) && Objects.nonNull(purchaseMinerProjectPageQry.getEndTime())) {
            lambdaQueryWrapper.between(PurchaseMinerProject::getCreateTime, purchaseMinerProjectPageQry.getStartTime(), purchaseMinerProjectPageQry.getEndTime());
        }

        Page<PurchaseMinerProject> purchaseMinerProjectPage = purchaseMinerProjectMapper.selectPage(Page.of(purchaseMinerProjectPageQry.getPageNum(), purchaseMinerProjectPageQry.getPageSize()), lambdaQueryWrapper);

        if (CollectionUtils.isEmpty(purchaseMinerProjectPage.getRecords())) {
            return MultiResponse.buildSuccess();
        }

        List<PurchaseMinerProjectDTO> purchaseMinerProjectDTOS = new ArrayList<>();

        String dayTime = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        for (PurchaseMinerProject purchaseMinerProject : purchaseMinerProjectPage.getRecords()) {
            PurchaseMinerProjectDTO purchaseMinerProjectDTO = new PurchaseMinerProjectDTO();
            BeanUtils.copyProperties(purchaseMinerProject, purchaseMinerProjectDTO);
            purchaseMinerProjectDTO.setTypeName(PurchaseMinerType.of(purchaseMinerProject.getType()).getName());
            purchaseMinerProjectDTO.setStatusName(PurchaseMinerProjectStatus.of(purchaseMinerProject.getStatus()).getName());


//            LambdaQueryWrapper<PurchaseMinerProjectReward> queryWrapper = new LambdaQueryWrapper<>();
//            queryWrapper.eq(PurchaseMinerProjectReward::getPurchaseMinerProjectId, purchaseMinerProject.getId());
//
//            List<PurchaseMinerProjectReward> rewardList = purchaseMinerProjectRewardMapper.selectList(queryWrapper);
//
//            BigDecimal totalReward = rewardList.stream()
//                    .map(PurchaseMinerProjectReward::getReward)
//                    .map(BigDecimal::new)
//                    .reduce(BigDecimal::add)
//                    .orElse(BigDecimal.ZERO);
//
//            BigDecimal yesterdayTotalReward = rewardList.stream()
//                    .filter(x -> x.getDayTime().equals(dayTime))
//                    .map(PurchaseMinerProjectReward::getReward)
//                    .map(BigDecimal::new)
//                    .reduce(BigDecimal::add)
//                    .orElse(BigDecimal.ZERO);

            LambdaQueryWrapper<MinerDailyReward> minerDailyRewardLambdaQueryWrapper = new LambdaQueryWrapper<>();
            minerDailyRewardLambdaQueryWrapper.eq(MinerDailyReward::getMinerProjectId,purchaseMinerProject.getId());
            minerDailyRewardLambdaQueryWrapper.eq(MinerDailyReward::getDayTime,dayTime);

            MinerDailyReward minerDailyReward = minerDailyRewardMapper.selectOne(minerDailyRewardLambdaQueryWrapper);
            if (Objects.isNull(minerDailyReward)){
                purchaseMinerProjectDTO.setYesterdayTotalRewardPrice("0");
                purchaseMinerProjectDTO.setYesterdayTotalReward("0");
            }else {
                purchaseMinerProjectDTO.setYesterdayTotalReward(minerDailyReward.getTotalReward());
                purchaseMinerProjectDTO.setYesterdayTotalRewardPrice(minerDailyReward.getTotalRewardPrice());
            }

            purchaseMinerProjectDTOS.add(purchaseMinerProjectDTO);
        }
        return MultiResponse.of(purchaseMinerProjectDTOS, (int) purchaseMinerProjectPage.getTotal());
    }

    @Override
    public SingleResponse<PurchaseMinerProjectStatisticsDTO> statistics() {

        // 获取昨天的日期
        LocalDate yesterday = LocalDate.now().minusDays(1);

        // 获取昨天的开始时间 (00:00:00.000)
        LocalDateTime startOfYesterday = yesterday.atStartOfDay();

        // 获取昨天的结束时间 (23:59:59.999)
        LocalDateTime endOfYesterday = yesterday.atTime(LocalTime.MAX);

        // 转换为时间戳（毫秒）
        Long startTimestamp = startOfYesterday.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        Long endTimestamp = endOfYesterday.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        PurchaseMinerProjectStatisticsDTO purchaseMinerProjectStatisticsDTO = new PurchaseMinerProjectStatisticsDTO();

        LambdaQueryWrapper<PurchaseMinerProject> purchaseMinerProjectLambdaQueryWrapper = new LambdaQueryWrapper<>();
        purchaseMinerProjectLambdaQueryWrapper.eq(PurchaseMinerProject::getStatus, PurchaseMinerProjectStatus.SUCCESS.getCode());

        List<PurchaseMinerProject> purchaseMinerProjectList = purchaseMinerProjectMapper.selectList(purchaseMinerProjectLambdaQueryWrapper);

        BigDecimal totalComputingPower = purchaseMinerProjectList.stream()
                .map(PurchaseMinerProject::getActualComputingPower)
                .map(BigDecimal::new)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        Integer totalPurchaseMinerProjectCount = purchaseMinerProjectList.size();

        List<PurchaseMinerProject> yesterdayPurchaseMinerProjectList = purchaseMinerProjectList.stream()
                .filter(x -> x.getCreateTime() > startTimestamp && x.getCreateTime() < endTimestamp)
                .collect(Collectors.toList());

        BigDecimal yesterdayTotalComputingPower = yesterdayPurchaseMinerProjectList
                .stream()
                .map(PurchaseMinerProject::getActualComputingPower)
                .map(BigDecimal::new)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        Integer yesterdayTotalPurchaseMinerProjectCount = yesterdayPurchaseMinerProjectList.size();


        List<RewardStatisticsLog> rewardStatisticsLogList = rewardStatisticsLogMapper.selectList(new QueryWrapper<>());

        BigDecimal totalEcoNumber = rewardStatisticsLogList.stream()
                .map(RewardStatisticsLog::getTotalReward)
                .map(BigDecimal::new)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        String dayTime = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        List<RewardStatisticsLog> yesterdayRewardStatisticsLogList = rewardStatisticsLogList.stream()
                .filter(x -> x.getDayTime().equals(dayTime))
                .collect(Collectors.toList());

        BigDecimal yesterdayTotalEcoNumber = yesterdayRewardStatisticsLogList.stream()
                .map(RewardStatisticsLog::getTotalReward)
                .map(BigDecimal::new)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);


        LambdaQueryWrapper<SystemConfig> priceLambdaQueryWrapper = new LambdaQueryWrapper<>();
        priceLambdaQueryWrapper.eq(SystemConfig::getName, SystemConfigEnum.ECO_PRICE.getCode());

        SystemConfig priceSystemConfig = systemConfigMapper.selectOne(priceLambdaQueryWrapper);
        if (Objects.isNull(priceSystemConfig)) {
            return SingleResponse.buildFailure("未设置ECO价格");
        }


        LambdaQueryWrapper<SystemConfig> totalNumberLambdaQueryWrapper = new LambdaQueryWrapper<>();
        totalNumberLambdaQueryWrapper.eq(SystemConfig::getName, SystemConfigEnum.ECO_TOTAL_NUMBER.getCode());

        SystemConfig totalNumberSystemConfig = systemConfigMapper.selectOne(totalNumberLambdaQueryWrapper);
        if (Objects.isNull(totalNumberSystemConfig)) {
            return SingleResponse.buildFailure("未设置ECO总发行量");
        }

        BigDecimal totalNumber = new BigDecimal(totalNumberSystemConfig.getValue());

        BigDecimal progress = totalEcoNumber.divide(totalNumber, 4, RoundingMode.HALF_UP);

        purchaseMinerProjectStatisticsDTO.setTotalComputingPower(totalComputingPower.toString());
        purchaseMinerProjectStatisticsDTO.setTotalPurchaseMinerProjectCount(totalPurchaseMinerProjectCount);
        purchaseMinerProjectStatisticsDTO.setProgress(progress.toString());
        purchaseMinerProjectStatisticsDTO.setPrice(priceSystemConfig.getValue());
        purchaseMinerProjectStatisticsDTO.setTotalEcoNumber(totalEcoNumber.toString());
        purchaseMinerProjectStatisticsDTO.setYesterdayTotalEcoNumber(yesterdayTotalEcoNumber.toString());
        purchaseMinerProjectStatisticsDTO.setYesterdayTotalComputingPower(yesterdayTotalComputingPower.toString());
        purchaseMinerProjectStatisticsDTO.setYesterdayTotalPurchaseMinerProjectCount(yesterdayTotalPurchaseMinerProjectCount);

        return SingleResponse.of(purchaseMinerProjectStatisticsDTO);
    }

    @Override
    public SingleResponse<PurchaseMinerProjectRewardDTO> reward(PurchaseMinerProjectRewardQry purchaseMinerProjectRewardQry) {

        LambdaQueryWrapper<PurchaseMinerProjectReward> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PurchaseMinerProjectReward::getWalletAddress,purchaseMinerProjectRewardQry.getWalletAddress());
        queryWrapper.eq(PurchaseMinerProjectReward::getDayTime,purchaseMinerProjectRewardQry.getDayTime());

        List<PurchaseMinerProjectReward> purchaseMinerProjectRewards = purchaseMinerProjectRewardMapper.selectList(queryWrapper);

        PurchaseMinerProjectRewardDTO purchaseMinerProjectRewardDTO = new PurchaseMinerProjectRewardDTO();
        if (CollectionUtils.isEmpty(purchaseMinerProjectRewards)){

            purchaseMinerProjectRewardDTO.setBaseReward("0");
            purchaseMinerProjectRewardDTO.setNewReward("0");
            purchaseMinerProjectRewardDTO.setDynamicReward("0");
            purchaseMinerProjectRewardDTO.setRecommendReward("0");
            purchaseMinerProjectRewardDTO.setStaticReward("0");
            purchaseMinerProjectRewardDTO.setBaseRewardPrice("0");
            purchaseMinerProjectRewardDTO.setDynamicRewardPrice("0");
            purchaseMinerProjectRewardDTO.setStaticRewardPrice("0");
            purchaseMinerProjectRewardDTO.setNewRewardPrice("0");
            purchaseMinerProjectRewardDTO.setRecommendRewardPrice("0");
        }

        BigDecimal totalStaticReward = purchaseMinerProjectRewards.stream()
                .filter(x -> x.getType().equals(PurchaseMinerProjectRewardType.STATIC.getCode()))
                .map(PurchaseMinerProjectReward::getReward)
                .map(BigDecimal::new)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalStaticRewardPrice = purchaseMinerProjectRewards.stream()
                .filter(x -> x.getType().equals(PurchaseMinerProjectRewardType.STATIC.getCode()))
                .map(PurchaseMinerProjectReward::getRewardPrice)
                .map(BigDecimal::new)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);


        BigDecimal totalDynamicReward = purchaseMinerProjectRewards.stream()
                .filter(x -> x.getType().equals(PurchaseMinerProjectRewardType.DYNAMIC.getCode()))
                .map(PurchaseMinerProjectReward::getReward)
                .map(BigDecimal::new)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalDynamicRewardPrice = purchaseMinerProjectRewards.stream()
                .filter(x -> x.getType().equals(PurchaseMinerProjectRewardType.DYNAMIC.getCode()))
                .map(PurchaseMinerProjectReward::getRewardPrice)
                .map(BigDecimal::new)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalDynamicRecommendReward = purchaseMinerProjectRewards.stream()
                .filter(x -> x.getType().equals(PurchaseMinerProjectRewardType.DYNAMIC.getCode()))
                .filter(x -> x.getRewardType().equals(PurchaseMinerProjectDynamicRewardType.RECOMMEND.getCode()))
                .map(PurchaseMinerProjectReward::getReward)
                .map(BigDecimal::new)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalDynamicRecommendRewardPrice = purchaseMinerProjectRewards.stream()
                .filter(x -> x.getType().equals(PurchaseMinerProjectRewardType.DYNAMIC.getCode()))
                .filter(x -> x.getRewardType().equals(PurchaseMinerProjectDynamicRewardType.RECOMMEND.getCode()))
                .map(PurchaseMinerProjectReward::getRewardPrice)
                .map(BigDecimal::new)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);


        BigDecimal totalDynamicBaseReward = purchaseMinerProjectRewards.stream()
                .filter(x -> x.getType().equals(PurchaseMinerProjectRewardType.DYNAMIC.getCode()))
                .filter(x -> x.getRewardType().equals(PurchaseMinerProjectDynamicRewardType.BASE.getCode()))
                .map(PurchaseMinerProjectReward::getReward)
                .map(BigDecimal::new)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalDynamicBaseRewardPrice = purchaseMinerProjectRewards.stream()
                .filter(x -> x.getType().equals(PurchaseMinerProjectRewardType.DYNAMIC.getCode()))
                .filter(x -> x.getRewardType().equals(PurchaseMinerProjectDynamicRewardType.BASE.getCode()))
                .map(PurchaseMinerProjectReward::getRewardPrice)
                .map(BigDecimal::new)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalDynamicNewReward = purchaseMinerProjectRewards.stream()
                .filter(x -> x.getType().equals(PurchaseMinerProjectRewardType.DYNAMIC.getCode()))
                .filter(x -> x.getRewardType().equals(PurchaseMinerProjectDynamicRewardType.NEW.getCode()))
                .map(PurchaseMinerProjectReward::getReward)
                .map(BigDecimal::new)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalDynamicNewRewardPrice = purchaseMinerProjectRewards.stream()
                .filter(x -> x.getType().equals(PurchaseMinerProjectRewardType.DYNAMIC.getCode()))
                .filter(x -> x.getRewardType().equals(PurchaseMinerProjectDynamicRewardType.NEW.getCode()))
                .map(PurchaseMinerProjectReward::getRewardPrice)
                .map(BigDecimal::new)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        purchaseMinerProjectRewardDTO.setDynamicReward(totalDynamicReward.toString());
        purchaseMinerProjectRewardDTO.setStaticReward(totalStaticReward.toString());
        purchaseMinerProjectRewardDTO.setBaseReward(totalDynamicBaseReward.toString());
        purchaseMinerProjectRewardDTO.setNewReward(totalDynamicNewReward.toString());
        purchaseMinerProjectRewardDTO.setRecommendReward(totalDynamicRecommendReward.toString());

        purchaseMinerProjectRewardDTO.setRecommendRewardPrice(totalDynamicRecommendRewardPrice.toString());
        purchaseMinerProjectRewardDTO.setDynamicRewardPrice(totalDynamicRewardPrice.toString());
        purchaseMinerProjectRewardDTO.setStaticRewardPrice(totalStaticRewardPrice.toString());
        purchaseMinerProjectRewardDTO.setBaseRewardPrice(totalDynamicBaseRewardPrice.toString());
        purchaseMinerProjectRewardDTO.setNewRewardPrice(totalDynamicNewRewardPrice.toString());

        return SingleResponse.of(purchaseMinerProjectRewardDTO);
    }

    @Override
    public SingleResponse<RewardServiceResultDTO> checkRewardService(RewardServiceQry rewardServiceQry) {

        RewardServiceResultDTO rewardServiceResultDTO = new RewardServiceResultDTO();

        String dayTime = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        LambdaQueryWrapper<RewardServiceLog> rewardServiceLogLambdaQueryWrapper = new LambdaQueryWrapper<>();
        rewardServiceLogLambdaQueryWrapper.eq(RewardServiceLog::getDayTime,dayTime);
        rewardServiceLogLambdaQueryWrapper.eq(RewardServiceLog::getWalletAddress,rewardServiceQry.getWalletAddress());

        RewardServiceLog rewardServiceLog = rewardServiceLogMapper.selectOne(rewardServiceLogLambdaQueryWrapper);

        if (Objects.isNull(rewardServiceLog)){
            rewardServiceResultDTO.setResult(Boolean.TRUE);
            return SingleResponse.of(rewardServiceResultDTO);
        }

        if (new BigDecimal(rewardServiceLog.getEcoNumber()).compareTo(BigDecimal.ZERO) > 0){
            rewardServiceResultDTO.setResult(Boolean.FALSE);
        }

        return SingleResponse.of(rewardServiceResultDTO);
    }
}
