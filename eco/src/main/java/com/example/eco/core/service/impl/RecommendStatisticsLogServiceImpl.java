package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.bean.dto.RecommendComputingPowerDTO;
import com.example.eco.bean.dto.RecommendStatisticsLogDTO;
import com.example.eco.common.PendOrderStatus;
import com.example.eco.core.service.RecommendStatisticsLogService;
import com.example.eco.model.entity.*;
import com.example.eco.model.mapper.RecommendMapper;
import com.example.eco.model.mapper.RecommendStatisticsLogMapper;
import com.example.eco.model.mapper.RewardLevelConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RecommendStatisticsLogServiceImpl implements RecommendStatisticsLogService {

    @Resource
    private RecommendStatisticsLogMapper recommendStatisticsLogMapper;
    @Resource
    private RecommendMapper recommendMapper;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private RewardLevelConfigMapper rewardLevelConfigMapper;


    private static final String RECOMMEND_STATISTICS_LOCK_KEY = "recommend_statistics_lock:";

    @Async
    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> statistics(DirectRecommendCountCmd directRecommendCountCmd) {

        RLock lock = redissonClient.getLock(RECOMMEND_STATISTICS_LOCK_KEY + directRecommendCountCmd.getWalletAddress());

        try {
            if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                return SingleResponse.buildFailure("推荐统计处理中，请稍后再试");
            }

            String dayTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            //被推荐人钱包地址
            LambdaQueryWrapper<RecommendStatisticsLog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(RecommendStatisticsLog::getWalletAddress,directRecommendCountCmd.getRecommendWalletAddress());

            List<RecommendStatisticsLog> recommendStatisticsLogs = recommendStatisticsLogMapper.selectList(queryWrapper);

            BigDecimal totalComputingPower = recommendStatisticsLogs.stream()
                    .map(RecommendStatisticsLog::getTotalComputingPower)
                    .map(BigDecimal::new)
                    .reduce(BigDecimal::add)
                    .orElse(BigDecimal.ZERO);

            BigDecimal totalRecommendTotalComputingPower = recommendStatisticsLogs.stream()
                    .map(RecommendStatisticsLog::getTotalRecommendComputingPower)
                    .map(BigDecimal::new)
                    .reduce(BigDecimal::add)
                    .orElse(BigDecimal.ZERO);

            totalRecommendTotalComputingPower = totalRecommendTotalComputingPower.add(totalComputingPower);

            //推荐人钱包地址
            RecommendStatisticsLog statisticsLog = getOrCreate(directRecommendCountCmd.getWalletAddress(), dayTime);
            statisticsLog.setDirectRecommendCount(statisticsLog.getDirectRecommendCount() + 1);

            BigDecimal newTotalDirectRecommendComputingPower = new BigDecimal(statisticsLog.getTotalDirectRecommendComputingPower()).add(totalComputingPower);
            statisticsLog.setTotalDirectRecommendComputingPower(newTotalDirectRecommendComputingPower.toString());

            BigDecimal newTotalRecommendComputingPower = new BigDecimal(statisticsLog.getTotalRecommendComputingPower()).add(totalRecommendTotalComputingPower);
            statisticsLog.setTotalRecommendComputingPower(newTotalRecommendComputingPower.toString());

            recommendStatisticsLogMapper.updateById(statisticsLog);


            // 递归更新上级的推荐人总算力
            updateTotalRecommendComputingPower(dayTime, directRecommendCountCmd.getWalletAddress(), totalRecommendTotalComputingPower);

            return SingleResponse.buildSuccess();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SingleResponse.buildFailure("操作被中断");
        } catch (Exception e) {
            return SingleResponse.buildFailure("删除挂单失败: " + e.getMessage());
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

//    @Async
    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> statistics(TotalComputingPowerCmd totalComputingPowerCmd) {
        RLock lock = redissonClient.getLock(RECOMMEND_STATISTICS_LOCK_KEY + totalComputingPowerCmd.getWalletAddress());

        try {
            if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                return SingleResponse.buildFailure("推荐统计处理中，请稍后再试");
            }

            String dayTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            RecommendStatisticsLog recommendStatisticsLog = getOrCreate(totalComputingPowerCmd.getWalletAddress(), dayTime);

            BigDecimal totalComputingPower = new BigDecimal(recommendStatisticsLog.getTotalComputingPower())
                    .add(new BigDecimal(totalComputingPowerCmd.getComputingPower()));

            recommendStatisticsLog.setTotalComputingPower(totalComputingPower.toString());
            recommendStatisticsLogMapper.updateById(recommendStatisticsLog);

            // 更新上级的推荐人总算力
            LambdaQueryWrapper<Recommend> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Recommend::getWalletAddress, totalComputingPowerCmd.getWalletAddress());
            queryWrapper.last("FOR UPDATE");

            Recommend recommend = recommendMapper.selectOne(queryWrapper);
            if (Objects.nonNull(recommend) && Objects.nonNull(recommend.getRecommendWalletAddress())) {

                // 有上级，更新上级的推荐人总算力
                RecommendStatisticsLog parentLog = getOrCreate(recommend.getRecommendWalletAddress(), dayTime);

                BigDecimal parentTotalDirectRecommendComputingPower = new BigDecimal(parentLog.getTotalDirectRecommendComputingPower())
                        .add(new BigDecimal(totalComputingPowerCmd.getComputingPower()));

                BigDecimal parentTotalRecommendComputingPower = new BigDecimal(parentLog.getTotalRecommendComputingPower())
                        .add(new BigDecimal(totalComputingPowerCmd.getComputingPower()));

                parentLog.setTotalRecommendComputingPower(parentTotalRecommendComputingPower.toString());
                parentLog.setTotalDirectRecommendComputingPower(parentTotalDirectRecommendComputingPower.toString());
                recommendStatisticsLogMapper.updateById(parentLog);

                // 递归更新上级的推荐人总算力
                updateTotalRecommendComputingPower(dayTime, recommend.getRecommendWalletAddress(), new BigDecimal(totalComputingPowerCmd.getComputingPower()));
            }


            return SingleResponse.buildSuccess();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("推荐统计失败");
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


    /**
     * 递归更新上级的推荐人总算力
     */
    public void updateTotalRecommendComputingPower(String dayTime, String walletAddress, BigDecimal computingPower) {

        LambdaQueryWrapper<Recommend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Recommend::getWalletAddress, walletAddress);
        queryWrapper.last("FOR UPDATE");

        Recommend recommend = recommendMapper.selectOne(queryWrapper);
        if (Objects.nonNull(recommend) && StringUtils.hasLength(recommend.getRecommendWalletAddress())) {
            // 有上级，更新上级的推荐人总算力
            RecommendStatisticsLog parentLog = getOrCreate(recommend.getRecommendWalletAddress(), dayTime);

            BigDecimal parentTotalRecommendComputingPower = new BigDecimal(parentLog.getTotalRecommendComputingPower()).add(computingPower);

            parentLog.setTotalRecommendComputingPower(parentTotalRecommendComputingPower.toString());

            recommendStatisticsLogMapper.updateById(parentLog);

            if (Objects.nonNull(recommend.getRecommendWalletAddress())) {
                // 递归更新上级的上级
                updateTotalRecommendComputingPower(dayTime, recommend.getRecommendWalletAddress(), computingPower);
            }
        }
    }

    @Override
    public SingleResponse<RecommendStatisticsLogDTO> get(RecommendStatisticsLogQry recommendStatisticsLogQry) {

        LambdaQueryWrapper<RecommendStatisticsLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StringUtils.hasLength(recommendStatisticsLogQry.getWalletAddress()), RecommendStatisticsLog::getWalletAddress, recommendStatisticsLogQry.getWalletAddress());

        List<RecommendStatisticsLog> recommendStatisticsLogs = recommendStatisticsLogMapper.selectList(queryWrapper);

        Integer directRecommendCount = recommendStatisticsLogs.stream()
                .map(RecommendStatisticsLog::getDirectRecommendCount)
                .reduce(0, Integer::sum);

        BigDecimal totalComputingPower = recommendStatisticsLogs.stream()
                .map(RecommendStatisticsLog::getTotalComputingPower)
                .map(BigDecimal::new)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDirectRecommendComputingPower = recommendStatisticsLogs.stream()
                .map(RecommendStatisticsLog::getTotalDirectRecommendComputingPower)
                .map(BigDecimal::new)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRecommendComputingPower = recommendStatisticsLogs.stream()
                .map(RecommendStatisticsLog::getTotalRecommendComputingPower)
                .map(BigDecimal::new)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        RecommendStatisticsLogDTO recommendStatisticsLogDTO = new RecommendStatisticsLogDTO();
        recommendStatisticsLogDTO.setWalletAddress(recommendStatisticsLogQry.getWalletAddress());
        recommendStatisticsLogDTO.setDirectRecommendCount(directRecommendCount);
        recommendStatisticsLogDTO.setTotalComputingPower(totalComputingPower.toString());
        recommendStatisticsLogDTO.setTotalDirectRecommendComputingPower(totalDirectRecommendComputingPower.toString());
        recommendStatisticsLogDTO.setTotalRecommendComputingPower(totalRecommendComputingPower.toString());


        LambdaQueryWrapper<Recommend> recommendQueryWrapper = new LambdaQueryWrapper<>();
        recommendQueryWrapper.eq(Recommend::getWalletAddress, recommendStatisticsLogQry.getWalletAddress());

        Recommend recommend = recommendMapper.selectOne(recommendQueryWrapper);
        if (Objects.isNull(recommend)) {
            recommendStatisticsLogDTO.setMaxComputingPower("0");
            recommendStatisticsLogDTO.setMinComputingPower("0");
            recommendStatisticsLogDTO.setNewComputingPower("0");

            return SingleResponse.of(recommendStatisticsLogDTO);
        }

        LambdaQueryWrapper<Recommend> directQueryWrapper = new LambdaQueryWrapper<>();
        directQueryWrapper.eq(Recommend::getRecommendWalletAddress, recommendStatisticsLogQry.getWalletAddress());

        List<Recommend> directRecommendList = recommendMapper.selectList(directQueryWrapper);
        if (CollectionUtils.isEmpty(directRecommendList)) {
            recommendStatisticsLogDTO.setMaxComputingPower("0");
            recommendStatisticsLogDTO.setMinComputingPower("0");
            recommendStatisticsLogDTO.setNewComputingPower("0");

            return SingleResponse.of(recommendStatisticsLogDTO);
        }

        List<String> directWalletAddressList = directRecommendList.stream()
                .map(Recommend::getWalletAddress)
                .collect(Collectors.toList());

        LambdaQueryWrapper<RecommendStatisticsLog> directRecommendStatisticsLogQueryWrapper = new LambdaQueryWrapper<>();
        directRecommendStatisticsLogQueryWrapper.in(RecommendStatisticsLog::getWalletAddress, directWalletAddressList);

        List<RecommendStatisticsLog> directRecommendStatisticsLogList = recommendStatisticsLogMapper.selectList(directRecommendStatisticsLogQueryWrapper);

        if (CollectionUtils.isEmpty(directRecommendStatisticsLogList)) {
            recommendStatisticsLogDTO.setMaxComputingPower("0");
            recommendStatisticsLogDTO.setMinComputingPower("0");
            recommendStatisticsLogDTO.setNewComputingPower("0");

            return SingleResponse.of(recommendStatisticsLogDTO);
        }

        Map<String, List<RecommendStatisticsLog>> directRecommendStatisticsLogMap = directRecommendStatisticsLogList.stream().collect(Collectors.groupingBy(RecommendStatisticsLog::getWalletAddress));

        Map<String, BigDecimal> totalRecommendComputingPowerMap = new HashMap<>();

        for (Map.Entry<String, List<RecommendStatisticsLog>> entry : directRecommendStatisticsLogMap.entrySet()) {
            String walletAddress = entry.getKey();
            List<RecommendStatisticsLog> logs = entry.getValue();

            BigDecimal totalPower = logs.stream()
                    .map(x -> new BigDecimal(x.getTotalComputingPower())
                            .add(new BigDecimal(x.getTotalRecommendComputingPower())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalRecommendComputingPowerMap.put(walletAddress, totalPower);
        }

        //添加最小算力和最大算力
        computingPower(recommendStatisticsLogDTO,
                levelRateMap(), 
                new HashMap<>(),
                totalRecommendComputingPowerMap,
                recommendStatisticsLogQry.getIsLevel(),
                recommendStatisticsLogQry.getDayTime()
        );

        return SingleResponse.of(recommendStatisticsLogDTO);

    }

    @Override
    public MultiResponse<RecommendStatisticsLogDTO> list(RecommendStatisticsLogListQry recommendStatisticsLogListQry) {


        LambdaQueryWrapper<RecommendStatisticsLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StringUtils.hasLength(recommendStatisticsLogListQry.getWalletAddress()), RecommendStatisticsLog::getWalletAddress, recommendStatisticsLogListQry.getWalletAddress());

        List<RecommendStatisticsLog> recommendStatisticsLogs = recommendStatisticsLogMapper.selectList(queryWrapper);

        Map<String, List<RecommendStatisticsLog>> recommendStatisticsLogMap = recommendStatisticsLogs.stream().collect(Collectors.groupingBy(RecommendStatisticsLog::getWalletAddress));

        List<RecommendStatisticsLogDTO> recommendStatisticsLogDTOS = new ArrayList<>();

        //获取等级配置
        Map<Integer, BigDecimal> levelRateMap = levelRateMap();

        Map<String, BigDecimal> computedPowerMap = new HashMap<>();

        Map<String, BigDecimal> totalRecommendComputingPowerMap = new HashMap<>();

        for (Map.Entry<String, List<RecommendStatisticsLog>> entry : recommendStatisticsLogMap.entrySet()) {

            String walletAddress = entry.getKey();

            List<RecommendStatisticsLog> logs = entry.getValue();

            Integer directRecommendCount = logs.stream()
                    .map(RecommendStatisticsLog::getDirectRecommendCount)
                    .reduce(0, Integer::sum);

            BigDecimal totalComputingPower = logs.stream()
                    .map(RecommendStatisticsLog::getTotalComputingPower)
                    .map(BigDecimal::new)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalRecommendComputingPower = logs.stream()
                    .map(RecommendStatisticsLog::getTotalRecommendComputingPower)
                    .map(BigDecimal::new)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalDirectRecommendComputingPower = logs.stream()
                    .map(RecommendStatisticsLog::getTotalDirectRecommendComputingPower)
                    .map(BigDecimal::new)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);


            computedPowerMap.put(walletAddress, totalComputingPower);

            totalRecommendComputingPowerMap.put(walletAddress, totalRecommendComputingPower.add(totalComputingPower));

            RecommendStatisticsLogDTO recommendStatisticsLogDTO = new RecommendStatisticsLogDTO();
            recommendStatisticsLogDTO.setWalletAddress(walletAddress);
            recommendStatisticsLogDTO.setDirectRecommendCount(directRecommendCount);
            recommendStatisticsLogDTO.setTotalComputingPower(totalComputingPower.toString());
            recommendStatisticsLogDTO.setTotalRecommendComputingPower(totalRecommendComputingPower.toString());
            recommendStatisticsLogDTO.setTotalDirectRecommendComputingPower(totalDirectRecommendComputingPower.toString());


            recommendStatisticsLogDTOS.add(recommendStatisticsLogDTO);
        }

        for (RecommendStatisticsLogDTO recommendStatisticsLogDTO : recommendStatisticsLogDTOS) {

            computingPower(recommendStatisticsLogDTO, 
                    levelRateMap, 
                    computedPowerMap,
                    totalRecommendComputingPowerMap,
                    recommendStatisticsLogListQry.getIsLevel(),
                    recommendStatisticsLogListQry.getDayTime());
        }


        return MultiResponse.of(recommendStatisticsLogDTOS);
    }


    /**
     * 获取或创建统计记录
     */
    public RecommendStatisticsLog getOrCreate(String walletAddress, String dayTime) {

        LambdaQueryWrapper<RecommendStatisticsLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RecommendStatisticsLog::getWalletAddress, walletAddress);
        queryWrapper.eq(RecommendStatisticsLog::getDayTime, dayTime);

        RecommendStatisticsLog existingLog = recommendStatisticsLogMapper.selectOne(queryWrapper);
        if (Objects.isNull(existingLog)) {

            existingLog = new RecommendStatisticsLog();
            existingLog.setWalletAddress(walletAddress);
            existingLog.setDayTime(dayTime);
            existingLog.setDirectRecommendCount(0);
            existingLog.setTotalComputingPower("0");
            existingLog.setTotalRecommendComputingPower("0");
            existingLog.setTotalDirectRecommendComputingPower("0");
            existingLog.setCreateTime(System.currentTimeMillis());
            recommendStatisticsLogMapper.insert(existingLog);
        }

        return existingLog;
    }

    /**
     * 计算算力
     */
    public void computingPower(RecommendStatisticsLogDTO recommendStatisticsLog,
                               Map<Integer, BigDecimal> levelRateMap,
                               Map<String, BigDecimal> computedPowerMap,
                               Map<String, BigDecimal> totalRecommendComputingPowerMap,
                               Boolean isLevel,
                               String dayTime) {

        LambdaQueryWrapper<Recommend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Recommend::getWalletAddress, recommendStatisticsLog.getWalletAddress());

        Recommend recommend = recommendMapper.selectOne(queryWrapper);
        if (Objects.isNull(recommend)) {
            return;
        }

        LambdaQueryWrapper<Recommend> directQueryWrapper = new LambdaQueryWrapper<>();
        directQueryWrapper.eq(Recommend::getRecommendWalletAddress, recommendStatisticsLog.getWalletAddress());

        List<Recommend> directRecommends = recommendMapper.selectList(directQueryWrapper);
        if (CollectionUtils.isEmpty(directRecommends)) {
            return;
        }

        if (directRecommends.size() < 2) {
            recommendStatisticsLog.setMinComputingPower("0");
            recommendStatisticsLog.setMaxComputingPower("0");
            recommendStatisticsLog.setNewComputingPower("0");
            return;
        }

        log.info("totalRecommendComputingPowerMap : {}",totalRecommendComputingPowerMap);

        String maxWalletAddress = totalRecommendComputingPowerMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (Objects.isNull(maxWalletAddress)){
            recommendStatisticsLog.setMinComputingPower("0");
            recommendStatisticsLog.setMaxComputingPower("0");
            recommendStatisticsLog.setNewComputingPower("0");
            return;
        }

        log.info("maxWalletAddress :{}",maxWalletAddress);

        String maxComputedPower = totalRecommendComputingPowerMap.getOrDefault(maxWalletAddress, BigDecimal.ZERO).toString();

        List<BigDecimal> computedPowerList = new ArrayList<>();

        List<BigDecimal> newComputedPowerList = new ArrayList<>();

        for (Recommend directRecommend : directRecommends) {

            log.info("directRecommend :{}",directRecommend.getWalletAddress());

            log.info("result :{}",directRecommend.getWalletAddress().equals(maxWalletAddress));

            if (directRecommend.getWalletAddress().equalsIgnoreCase(maxWalletAddress)) {
                continue; // 跳过最大算力的直推用户
            }

            BigDecimal computedPower = calculateSubordinates(recommend.getLevel(),
                    directRecommend.getWalletAddress(),
                    levelRateMap,
                    computedPowerMap,
                    null,
                    isLevel);

            BigDecimal newComputedPower = calculateSubordinates(recommend.getLevel(),
                    directRecommend.getWalletAddress(),
                    levelRateMap,
                    new HashMap<>(),
                    dayTime,
                    isLevel);

            newComputedPowerList.add(newComputedPower);

            computedPowerList.add(computedPower);
        }

        computedPowerList.sort(Comparator.reverseOrder());


        if (CollectionUtils.isEmpty(computedPowerList)) {
            recommendStatisticsLog.setMinComputingPower("0");
            recommendStatisticsLog.setMaxComputingPower("0");
        } else {

            BigDecimal minComputingPower = computedPowerList
                    .stream().reduce(BigDecimal::add)
                    .orElse(BigDecimal.ZERO);


            recommendStatisticsLog.setMinComputingPower(minComputingPower.toString());
            recommendStatisticsLog.setMaxComputingPower(maxComputedPower);
        }

        if (CollectionUtils.isEmpty(newComputedPowerList)){
            recommendStatisticsLog.setNewComputingPower("0");
        }else {

            BigDecimal newComputingPower = newComputedPowerList.stream()
                    .reduce(BigDecimal::add)
                    .orElse(BigDecimal.ZERO);

            recommendStatisticsLog.setNewComputingPower(newComputingPower.toString());
        }

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


    /**
     * 递归计算所有子级的算力总和
     */
    public BigDecimal calculateSubordinates(Integer parentLevel,
                                            String walletAddress,
                                            Map<Integer, BigDecimal> levelRateMap,
                                            Map<String, BigDecimal> computedPowerMap,
                                            String dayTime,
                                            Boolean isLevel) {
        // 1. 查询直接下级
        BigDecimal currentPower = computedPowerMap.get(walletAddress);

        if (Objects.isNull(currentPower)) {

            // 2. 获取当前地址的算力
            currentPower = BigDecimal.ZERO;

            LambdaQueryWrapper<RecommendStatisticsLog> recommendStatisticsLogLambdaQueryWrapper = new LambdaQueryWrapper<>();
            recommendStatisticsLogLambdaQueryWrapper.eq(RecommendStatisticsLog::getWalletAddress, walletAddress);
            
            if (StringUtils.hasLength(dayTime)){
                recommendStatisticsLogLambdaQueryWrapper.eq(RecommendStatisticsLog::getDayTime, dayTime);
            }

            List<RecommendStatisticsLog> recommendStatisticsLogList = recommendStatisticsLogMapper.selectList(recommendStatisticsLogLambdaQueryWrapper);

            if (!CollectionUtils.isEmpty(recommendStatisticsLogList)) {
                currentPower = recommendStatisticsLogList.stream()
                        .map(RecommendStatisticsLog::getTotalComputingPower)
                        .map(BigDecimal::new)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            computedPowerMap.put(walletAddress, currentPower);
        }

        BigDecimal totalSubordinatePower = currentPower;

        // 3. 处理每个直接下级

        LambdaQueryWrapper<Recommend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Recommend::getRecommendWalletAddress, walletAddress);
        List<Recommend> directSubordinates = recommendMapper.selectList(wrapper);

        for (Recommend subordinate : directSubordinates) {

            String subordinateAddress = subordinate.getWalletAddress();

            Integer level = subordinate.getLevel();

            BigDecimal levelRate = BigDecimal.ZERO;
            //是否按层级计算
            if (isLevel){

                // 计算当前下级相对于父级的层级差
                Integer relativeLevel = parentLevel - level;

                levelRate = levelRateMap.get(relativeLevel);

                if (Objects.isNull(levelRate)) {
                    continue;
                }
            }

            // 4. 递归查询当前下级的子级算力
            BigDecimal grandchildrenPower = calculateSubordinates(level, subordinateAddress, levelRateMap, computedPowerMap, dayTime, isLevel);

            // 5. 计算当前层级的加权算力

            BigDecimal weightedPower = grandchildrenPower;

            if (levelRate.compareTo(BigDecimal.ZERO) < 0) {

                weightedPower = grandchildrenPower.multiply(levelRate);

                computedPowerMap.put(subordinateAddress, weightedPower);
                
            }

            // 6. 累加到总算力
            totalSubordinatePower = totalSubordinatePower.add(weightedPower);
        }

        return totalSubordinatePower;
    }


}
