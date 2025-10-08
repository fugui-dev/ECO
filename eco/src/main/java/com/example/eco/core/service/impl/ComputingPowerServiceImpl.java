package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.dto.ComputingPowerDTO;
import com.example.eco.common.PurchaseMinerProjectStatus;
import com.example.eco.core.service.ComputingPowerService;
import com.example.eco.model.entity.PurchaseMinerProject;
import com.example.eco.model.entity.Recommend;
import com.example.eco.model.entity.RewardLevelConfig;
import com.example.eco.model.mapper.PurchaseMinerProjectMapper;
import com.example.eco.model.mapper.RecommendMapper;
import com.example.eco.model.mapper.RewardLevelConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service("computingPowerService")
public class ComputingPowerServiceImpl implements ComputingPowerService {

    @Resource
    private PurchaseMinerProjectMapper purchaseMinerProjectMapper;
    @Resource
    private RecommendMapper recommendMapper;
    @Resource
    private RewardLevelConfigMapper rewardLevelConfigMapper;

    @Override
    public SingleResponse<BigDecimal> calculateUserTotalPower(String walletAddress, String dayTime) {
        try {
            // 计算自身算力
            SingleResponse<BigDecimal> selfPowerResponse = calculateUserSelfPower(walletAddress, dayTime);
            if (!selfPowerResponse.isSuccess()) {
                return selfPowerResponse;
            }
            BigDecimal selfPower = selfPowerResponse.getData();

            // 计算推荐算力
            SingleResponse<BigDecimal> recommendPowerResponse = calculateUserRecommendPower(walletAddress, dayTime);
            if (!recommendPowerResponse.isSuccess()) {
                return recommendPowerResponse;
            }
            BigDecimal recommendPower = recommendPowerResponse.getData();

            // 总算力 = 自身算力 + 推荐算力
            BigDecimal totalPower = selfPower.add(recommendPower);

            log.debug("用户{}总算力计算: 自身={}, 推荐={}, 总计={}", walletAddress, selfPower, recommendPower, totalPower);
            return SingleResponse.of(totalPower);

        } catch (Exception e) {
            log.error("计算用户{}总算力失败", walletAddress, e);
            return SingleResponse.buildFailure("计算总算力失败: " + e.getMessage());
        }
    }

    @Override
    public SingleResponse<BigDecimal> calculateUserSelfPower(String walletAddress, String dayTime) {
        try {
            // 查询用户所有有效矿机
            LambdaQueryWrapper<PurchaseMinerProject> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PurchaseMinerProject::getWalletAddress, walletAddress);
            queryWrapper.eq(PurchaseMinerProject::getStatus, PurchaseMinerProjectStatus.SUCCESS.getCode());
            
            // 如果指定了日期，只计算该日期及之前的矿机
            if (dayTime != null && !dayTime.trim().isEmpty()) {
                long endTime = LocalDate.parse(dayTime).plusDays(1).atStartOfDay()
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                queryWrapper.le(PurchaseMinerProject::getCreateTime, endTime);
            }

            List<PurchaseMinerProject> miners = purchaseMinerProjectMapper.selectList(queryWrapper);
            
            if (CollectionUtils.isEmpty(miners)) {
                return SingleResponse.of(BigDecimal.ZERO);
            }

            // 计算自身算力总和
            BigDecimal selfPower = miners.stream()
                    .map(miner -> new BigDecimal(miner.getActualComputingPower()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            log.debug("用户{}自身算力: {}", walletAddress, selfPower);
            return SingleResponse.of(selfPower);

        } catch (Exception e) {
            log.error("计算用户{}自身算力失败", walletAddress, e);
            return SingleResponse.buildFailure("计算自身算力失败: " + e.getMessage());
        }
    }

    @Override
    public SingleResponse<BigDecimal> calculateUserRecommendPower(String walletAddress, String dayTime) {
        try {
            // 查询直接下级
            LambdaQueryWrapper<Recommend> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Recommend::getRecommendWalletAddress, walletAddress);
            
            List<Recommend> directRecommends = recommendMapper.selectList(queryWrapper);
            
            if (CollectionUtils.isEmpty(directRecommends)) {
                return SingleResponse.of(BigDecimal.ZERO);
            }

            // 计算所有下级的总算力（直推 + 所有子级）
            BigDecimal recommendPower = BigDecimal.ZERO;
            for (Recommend directRecommend : directRecommends) {
                BigDecimal subordinatePower = calculateSubordinatePower(directRecommend.getWalletAddress(), dayTime, new HashMap<>());
                recommendPower = recommendPower.add(subordinatePower);
            }
            
            log.debug("用户{}推荐算力: {}", walletAddress, recommendPower);
            return SingleResponse.of(recommendPower);

        } catch (Exception e) {
            log.error("计算用户{}推荐算力失败", walletAddress, e);
            return SingleResponse.buildFailure("计算推荐算力失败: " + e.getMessage());
        }
    }

    @Override
    public SingleResponse<BigDecimal> calculateUserDirectRecommendPower(String walletAddress, String dayTime) {
        try {
            // 查询直接下级
            LambdaQueryWrapper<Recommend> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Recommend::getRecommendWalletAddress, walletAddress);
            
            List<Recommend> directRecommends = recommendMapper.selectList(queryWrapper);
            
            if (CollectionUtils.isEmpty(directRecommends)) {
                return SingleResponse.of(BigDecimal.ZERO);
            }

            // 计算每个直接下级的自身算力（不包括下级的下级）
            BigDecimal directRecommendPower = BigDecimal.ZERO;
            for (Recommend directRecommend : directRecommends) {
                SingleResponse<BigDecimal> selfPowerResponse = calculateUserSelfPower(directRecommend.getWalletAddress(), dayTime);
                if (selfPowerResponse.isSuccess()) {
                    directRecommendPower = directRecommendPower.add(selfPowerResponse.getData());
                }
            }

            log.debug("用户{}直推算力: {}", walletAddress, directRecommendPower);
            return SingleResponse.of(directRecommendPower);

        } catch (Exception e) {
            log.error("计算用户{}直推算力失败", walletAddress, e);
            return SingleResponse.buildFailure("计算直推算力失败: " + e.getMessage());
        }
    }

    @Override
    public SingleResponse<BigDecimal> calculateUserMinPower(String walletAddress, String dayTime) {
        try {
            // 查询直接下级
            LambdaQueryWrapper<Recommend> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Recommend::getRecommendWalletAddress, walletAddress);
            
            List<Recommend> directRecommends = recommendMapper.selectList(queryWrapper);
            
            if (CollectionUtils.isEmpty(directRecommends) || directRecommends.size() < 2) {
                return SingleResponse.of(BigDecimal.ZERO);
            }

            // 计算每个直接下级的总算力
            Map<String, BigDecimal> directPowerMap = new HashMap<>();
            for (Recommend directRecommend : directRecommends) {
                SingleResponse<BigDecimal> totalPowerResponse = calculateUserTotalPower(directRecommend.getWalletAddress(), dayTime);
                if (totalPowerResponse.isSuccess()) {
                    directPowerMap.put(directRecommend.getWalletAddress(), totalPowerResponse.getData());
                }
            }

            // 找到最大算力的用户
            String maxWalletAddress = directPowerMap.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

            if (maxWalletAddress == null) {
                return SingleResponse.of(BigDecimal.ZERO);
            }

            // 计算除最大算力外的其他直推算力总和
            BigDecimal minPower = directPowerMap.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(maxWalletAddress))
                    .map(Map.Entry::getValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            log.debug("用户{}小区算力: 最大用户={}, 小区算力={}", walletAddress, maxWalletAddress, minPower);
            return SingleResponse.of(minPower);

        } catch (Exception e) {
            log.error("计算用户{}小区算力失败", walletAddress, e);
            return SingleResponse.buildFailure("计算小区算力失败: " + e.getMessage());
        }
    }

    @Override
    public SingleResponse<BigDecimal> calculateUserNewPower(String walletAddress, String dayTime) {
        try {
            if (dayTime == null || dayTime.trim().isEmpty()) {
                return SingleResponse.of(BigDecimal.ZERO);
            }

            // 查询当日新增的矿机
            long startTime = LocalDate.parse(dayTime).atStartOfDay()
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endTime = startTime + 24 * 60 * 60 * 1000 - 1;

            LambdaQueryWrapper<PurchaseMinerProject> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PurchaseMinerProject::getWalletAddress, walletAddress);
            queryWrapper.eq(PurchaseMinerProject::getStatus, PurchaseMinerProjectStatus.SUCCESS.getCode());
            queryWrapper.ge(PurchaseMinerProject::getCreateTime, startTime);
            queryWrapper.le(PurchaseMinerProject::getCreateTime, endTime);

            List<PurchaseMinerProject> newMiners = purchaseMinerProjectMapper.selectList(queryWrapper);
            
            if (CollectionUtils.isEmpty(newMiners)) {
                return SingleResponse.of(BigDecimal.ZERO);
            }

            // 计算新增算力
            BigDecimal newPower = newMiners.stream()
                    .map(miner -> new BigDecimal(miner.getActualComputingPower()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            log.debug("用户{}新增算力: {}", walletAddress, newPower);
            return SingleResponse.of(newPower);

        } catch (Exception e) {
            log.error("计算用户{}新增算力失败", walletAddress, e);
            return SingleResponse.buildFailure("计算新增算力失败: " + e.getMessage());
        }
    }

    @Override
    public SingleResponse<ComputingPowerDTO> getComputingPowerInfo(String walletAddress, String dayTime) {
        try {
            // 计算各种算力
            SingleResponse<BigDecimal> selfPowerResponse = calculateUserSelfPower(walletAddress, dayTime);
            SingleResponse<BigDecimal> totalPowerResponse = calculateUserTotalPower(walletAddress, dayTime);
            SingleResponse<BigDecimal> recommendPowerResponse = calculateUserRecommendPower(walletAddress, dayTime);
            SingleResponse<BigDecimal> directRecommendPowerResponse = calculateUserDirectRecommendPower(walletAddress, dayTime);
            SingleResponse<BigDecimal> minPowerResponse = calculateUserMinPower(walletAddress, dayTime, getLevelRateMap(), Boolean.TRUE);
            SingleResponse<BigDecimal> newPowerResponse = calculateUserNewPower(walletAddress, dayTime, getLevelRateMap(), Boolean.TRUE);

            if (!selfPowerResponse.isSuccess() || !totalPowerResponse.isSuccess() || 
                !recommendPowerResponse.isSuccess() || !directRecommendPowerResponse.isSuccess() ||
                !minPowerResponse.isSuccess() || !newPowerResponse.isSuccess()) {
                return SingleResponse.buildFailure("计算算力信息失败");
            }

            // 获取推荐信息
            LambdaQueryWrapper<Recommend> recommendQuery = new LambdaQueryWrapper<>();
            recommendQuery.eq(Recommend::getWalletAddress, walletAddress);
            Recommend recommend = recommendMapper.selectOne(recommendQuery);

            // 计算直推人数
            LambdaQueryWrapper<Recommend> directQuery = new LambdaQueryWrapper<>();
            directQuery.eq(Recommend::getRecommendWalletAddress, walletAddress);
            List<Recommend> directRecommends = recommendMapper.selectList(directQuery);
            int directRecommendCount = directRecommends.size();

            // 找到最大算力的直推用户
            String maxWalletAddress = null;
            BigDecimal maxPower = BigDecimal.ZERO;
            if (!CollectionUtils.isEmpty(directRecommends)) {
                for (Recommend directRecommend : directRecommends) {
                    SingleResponse<BigDecimal> powerResponse = calculateUserTotalPower(directRecommend.getWalletAddress(), dayTime);
                    if (powerResponse.isSuccess() && powerResponse.getData().compareTo(maxPower) > 0) {
                        maxPower = powerResponse.getData();
                        maxWalletAddress = directRecommend.getWalletAddress();
                    }
                }
            }

            ComputingPowerDTO dto = ComputingPowerDTO.builder()
                    .walletAddress(walletAddress)
                    .selfPower(selfPowerResponse.getData())
                    .totalPower(totalPowerResponse.getData())
                    .recommendPower(recommendPowerResponse.getData())
                    .directRecommendPower(directRecommendPowerResponse.getData())
                    .minPower(minPowerResponse.getData())
                    .maxPower(maxPower)
                    .maxPowerWalletAddress(maxWalletAddress)
                    .newPower(newPowerResponse.getData())
                    .directRecommendCount(directRecommendCount)
                    .level(recommend != null ? recommend.getLevel() : 0)
                    .build();

            return SingleResponse.of(dto);

        } catch (Exception e) {
            log.error("获取用户{}算力信息失败", walletAddress, e);
            return SingleResponse.buildFailure("获取算力信息失败: " + e.getMessage());
        }
    }

    @Override
    public SingleResponse<BigDecimal> calculateTotalPower(String dayTime) {
        try {
            // 查询所有用户
            LambdaQueryWrapper<Recommend> queryWrapper = new LambdaQueryWrapper<>();
            List<Recommend> allUsers = recommendMapper.selectList(queryWrapper);
            
            if (CollectionUtils.isEmpty(allUsers)) {
                return SingleResponse.of(BigDecimal.ZERO);
            }

            // 计算所有用户的自身算力总和
            BigDecimal totalPower = BigDecimal.ZERO;
            for (Recommend user : allUsers) {
                SingleResponse<BigDecimal> selfPowerResponse = calculateUserSelfPower(user.getWalletAddress(), dayTime);
                if (selfPowerResponse.isSuccess()) {
                    totalPower = totalPower.add(selfPowerResponse.getData());
                }
            }

            log.debug("总算力: {}", totalPower);
            return SingleResponse.of(totalPower);

        } catch (Exception e) {
            log.error("计算总算力失败", e);
            return SingleResponse.buildFailure("计算总算力失败: " + e.getMessage());
        }
    }

    /**
     * 递归计算下级算力
     */
    private BigDecimal calculateSubordinatePower(String walletAddress, String dayTime, Map<String, BigDecimal> cache) {
        // 检查缓存
        if (cache.containsKey(walletAddress)) {
            return cache.get(walletAddress);
        }

        // 计算当前用户的自身算力
        SingleResponse<BigDecimal> selfPowerResponse = calculateUserSelfPower(walletAddress, dayTime);
        BigDecimal currentPower = selfPowerResponse.isSuccess() ? selfPowerResponse.getData() : BigDecimal.ZERO;

        // 查询直接下级
        LambdaQueryWrapper<Recommend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Recommend::getRecommendWalletAddress, walletAddress);
        List<Recommend> directSubordinates = recommendMapper.selectList(queryWrapper);

        // 递归计算每个下级的算力
        BigDecimal subordinatePower = BigDecimal.ZERO;
        for (Recommend subordinate : directSubordinates) {
            BigDecimal subPower = calculateSubordinatePower(subordinate.getWalletAddress(), dayTime, cache);
            subordinatePower = subordinatePower.add(subPower);
        }

        // 总算力 = 自身算力 + 下级算力
        BigDecimal totalPower = currentPower.add(subordinatePower);
        
        // 缓存结果
        cache.put(walletAddress, totalPower);
        
        return totalPower;
    }

    /**
     * 获取各个层级的奖励比例
     */
    private Map<Integer, BigDecimal> getLevelRateMap() {
        List<RewardLevelConfig> levelConfigs = rewardLevelConfigMapper.selectList(new LambdaQueryWrapper<>());
        Map<Integer, BigDecimal> levelRateMap = new HashMap<>();
        
        for (RewardLevelConfig config : levelConfigs) {
            levelRateMap.put(config.getLevel(), new BigDecimal(config.getRewardRate()));
        }
        
        return levelRateMap;
    }

    @Override
    public SingleResponse<BigDecimal> calculateUserRecommendPower(String walletAddress, String dayTime, Map<Integer, BigDecimal> levelRateMap, Boolean isLevel) {
        // 推荐算力计算不需要阶梯计算，直接调用原有方法
        return calculateUserRecommendPower(walletAddress, dayTime);
    }

    @Override
    public SingleResponse<BigDecimal> calculateUserMinPower(String walletAddress, String dayTime, Map<Integer, BigDecimal> levelRateMap, Boolean isLevel) {
        try {
            // 查询直接下级
            LambdaQueryWrapper<Recommend> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Recommend::getRecommendWalletAddress, walletAddress);
            
            List<Recommend> directRecommends = recommendMapper.selectList(queryWrapper);
            
            if (CollectionUtils.isEmpty(directRecommends) || directRecommends.size() < 2) {
                return SingleResponse.of(BigDecimal.ZERO);
            }

            // 获取用户层级信息
            LambdaQueryWrapper<Recommend> userQuery = new LambdaQueryWrapper<>();
            userQuery.eq(Recommend::getWalletAddress, walletAddress);
            Recommend userRecommend = recommendMapper.selectOne(userQuery);
            Integer userLevel = userRecommend != null ? userRecommend.getLevel() : 0;

            // 计算每个直接下级的总算力 - 支持阶梯计算
            Map<String, BigDecimal> directPowerMap = new HashMap<>();
            Map<String, BigDecimal> computedPowerMap = new HashMap<>();
            
            for (Recommend directRecommend : directRecommends) {
                // 使用当前用户层级作为父级层级，计算直推用户及其下级的算力
                BigDecimal totalPower = calculateSubordinatePowerWithLevel(
                    userLevel, 
                    directRecommend.getWalletAddress(), 
                    dayTime, 
                    levelRateMap, 
                    computedPowerMap, 
                    isLevel
                );
                directPowerMap.put(directRecommend.getWalletAddress(), totalPower);
            }

            // 找到最大算力的用户
            String maxWalletAddress = directPowerMap.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

            if (maxWalletAddress == null) {
                return SingleResponse.of(BigDecimal.ZERO);
            }

            // 计算除最大算力外的其他直推算力总和
            BigDecimal minPower = directPowerMap.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(maxWalletAddress))
                    .map(Map.Entry::getValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            log.debug("用户{}小区算力(阶梯计算): 最大用户={}, 小区算力={}", walletAddress, maxWalletAddress, minPower);
            return SingleResponse.of(minPower);

        } catch (Exception e) {
            log.error("计算用户{}小区算力失败(阶梯计算)", walletAddress, e);
            return SingleResponse.buildFailure("计算小区算力失败: " + e.getMessage());
        }
    }

    @Override
    public SingleResponse<BigDecimal> calculateUserNewPower(String walletAddress, String dayTime, Map<Integer, BigDecimal> levelRateMap, Boolean isLevel) {
        try {
            if (dayTime == null || dayTime.trim().isEmpty()) {
                return SingleResponse.of(BigDecimal.ZERO);
            }

            // 查询当日新增的矿机
            long startTime = LocalDate.parse(dayTime).atStartOfDay()
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endTime = startTime + 24 * 60 * 60 * 1000 - 1;

            LambdaQueryWrapper<PurchaseMinerProject> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PurchaseMinerProject::getWalletAddress, walletAddress);
            queryWrapper.eq(PurchaseMinerProject::getStatus, PurchaseMinerProjectStatus.SUCCESS.getCode());
            queryWrapper.ge(PurchaseMinerProject::getCreateTime, startTime);
            queryWrapper.le(PurchaseMinerProject::getCreateTime, endTime);

            List<PurchaseMinerProject> newMiners = purchaseMinerProjectMapper.selectList(queryWrapper);
            
            if (CollectionUtils.isEmpty(newMiners)) {
                return SingleResponse.of(BigDecimal.ZERO);
            }

            // 计算新增算力 - 支持阶梯计算
            BigDecimal newPower = BigDecimal.ZERO;
            
            if (isLevel && levelRateMap != null && !levelRateMap.isEmpty()) {
                // 获取用户层级信息
                LambdaQueryWrapper<Recommend> userQuery = new LambdaQueryWrapper<>();
                userQuery.eq(Recommend::getWalletAddress, walletAddress);
                Recommend userRecommend = recommendMapper.selectOne(userQuery);
                Integer userLevel = userRecommend != null ? userRecommend.getLevel() : 0;
                
                // 获取推荐人层级信息
                Integer recommenderLevel = 0;
                if (userRecommend != null && userRecommend.getRecommendWalletAddress() != null) {
                    LambdaQueryWrapper<Recommend> recommenderQuery = new LambdaQueryWrapper<>();
                    recommenderQuery.eq(Recommend::getWalletAddress, userRecommend.getRecommendWalletAddress());
                    Recommend recommender = recommendMapper.selectOne(recommenderQuery);
                    recommenderLevel = recommender != null ? recommender.getLevel() : 0;
                }
                
                // 计算层级差：当前用户相对于推荐人的层级差
                Integer relativeLevel = userLevel - recommenderLevel;
                
                // 根据层级差计算新增算力
                for (PurchaseMinerProject miner : newMiners) {
                    BigDecimal basePower = new BigDecimal(miner.getActualComputingPower());
                    BigDecimal levelRate = levelRateMap.getOrDefault(relativeLevel, BigDecimal.ONE);
                    newPower = newPower.add(basePower.multiply(levelRate));
                }
            } else {
                // 不按层级计算，直接累加
                newPower = newMiners.stream()
                        .map(miner -> new BigDecimal(miner.getActualComputingPower()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            log.debug("用户{}新增算力(阶梯计算): {}", walletAddress, newPower);
            return SingleResponse.of(newPower);

        } catch (Exception e) {
            log.error("计算用户{}新增算力失败(阶梯计算)", walletAddress, e);
            return SingleResponse.buildFailure("计算新增算力失败: " + e.getMessage());
        }
    }

    /**
     * 递归计算下级算力 - 支持阶梯计算
     */
    private BigDecimal calculateSubordinatePowerWithLevel(Integer parentLevel, String walletAddress, String dayTime, 
                                                         Map<Integer, BigDecimal> levelRateMap, 
                                                         Map<String, BigDecimal> cache, Boolean isLevel) {
        // 检查缓存
        if (cache.containsKey(walletAddress)) {
            return cache.get(walletAddress);
        }

        // 计算当前用户的自身算力
        SingleResponse<BigDecimal> selfPowerResponse = calculateUserSelfPower(walletAddress, dayTime);
        BigDecimal currentPower = selfPowerResponse.isSuccess() ? selfPowerResponse.getData() : BigDecimal.ZERO;

        // 查询直接下级
        LambdaQueryWrapper<Recommend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Recommend::getRecommendWalletAddress, walletAddress);
        List<Recommend> directSubordinates = recommendMapper.selectList(queryWrapper);

        // 递归计算每个下级的算力
        BigDecimal subordinatePower = BigDecimal.ZERO;
        for (Recommend subordinate : directSubordinates) {
            String subordinateAddress = subordinate.getWalletAddress();
            Integer subordinateLevel = subordinate.getLevel();
            
            // 计算下级算力
            BigDecimal subPower = calculateSubordinatePowerWithLevel(
                subordinateLevel, 
                subordinateAddress, 
                dayTime, 
                levelRateMap, 
                cache, 
                isLevel
            );
            
            // 应用阶梯计算
            if (isLevel && levelRateMap != null && !levelRateMap.isEmpty()) {
                // 计算当前下级相对于父级的层级差
                // 如果父级是3级，下级是4级，则层级差为1（下级比父级低1级）
                Integer relativeLevel = subordinateLevel - parentLevel;
                BigDecimal levelRate = levelRateMap.get(relativeLevel);
                
                if (levelRate != null && levelRate.compareTo(BigDecimal.ZERO) > 0) {
                    subPower = subPower.multiply(levelRate);
                }
            }
            
            subordinatePower = subordinatePower.add(subPower);
        }

        // 总算力 = 自身算力 + 下级算力
        BigDecimal totalPower = currentPower.add(subordinatePower);
        
        // 缓存结果
        cache.put(walletAddress, totalPower);
        
        return totalPower;
    }

    /**
     * 获取各个层级的奖励比例
     */
    public Map<Integer, BigDecimal> levelRateMap() {

        List<RewardLevelConfig> levelConfigs = rewardLevelConfigMapper.selectList(new LambdaQueryWrapper<>());

        Map<Integer, BigDecimal> levelRateMap = new HashMap<>();

        for (RewardLevelConfig config : levelConfigs) {

            levelRateMap.put(config.getLevel(), new BigDecimal(config.getRewardRate()));
        }

        return levelRateMap;
    }
}