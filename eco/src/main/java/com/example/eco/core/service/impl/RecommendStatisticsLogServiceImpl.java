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
            RecommendStatisticsLog recommendStatisticsLog = getOrCreateAccount(directRecommendCountCmd.getRecommendWalletAddress(), dayTime);

            BigDecimal totalDirectRecommendComputingPower = new BigDecimal(recommendStatisticsLog.getTotalDirectRecommendComputingPower());

            //推荐人钱包地址
            RecommendStatisticsLog statisticsLog = getOrCreateAccount(directRecommendCountCmd.getWalletAddress(), dayTime);
            statisticsLog.setDirectRecommendCount(statisticsLog.getDirectRecommendCount() + 1);

            BigDecimal newTotalDirectRecommendComputingPower = new BigDecimal(statisticsLog.getTotalDirectRecommendComputingPower()).add(totalDirectRecommendComputingPower);
            statisticsLog.setTotalDirectRecommendComputingPower(newTotalDirectRecommendComputingPower.toString());
            recommendStatisticsLogMapper.updateById(statisticsLog);

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

    @Async
    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public SingleResponse<Void> statistics(TotalComputingPowerCmd totalComputingPowerCmd) {
        RLock lock = redissonClient.getLock(RECOMMEND_STATISTICS_LOCK_KEY + totalComputingPowerCmd.getWalletAddress());

        try {
            if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                return SingleResponse.buildFailure("推荐统计处理中，请稍后再试");
            }

            String dayTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            RecommendStatisticsLog recommendStatisticsLog = getOrCreateAccount(totalComputingPowerCmd.getWalletAddress(), dayTime);

            BigDecimal totalComputingPower = new BigDecimal(recommendStatisticsLog.getTotalComputingPower())
                    .add(new BigDecimal(totalComputingPowerCmd.getComputingPower()));

            recommendStatisticsLog.setTotalComputingPower(totalComputingPower.toString());
            recommendStatisticsLogMapper.updateById(recommendStatisticsLog);

            // 更新上级的推荐人总算力
            LambdaQueryWrapper<Recommend> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Recommend::getWalletAddress, totalComputingPowerCmd.getWalletAddress());
            queryWrapper.last("FOR UPDATE");

            Recommend recommend = recommendMapper.selectOne(queryWrapper);
            if(Objects.nonNull(recommend) && Objects.nonNull(recommend.getRecommendWalletAddress())) {

                // 有上级，更新上级的推荐人总算力
                RecommendStatisticsLog parentLog = getOrCreateAccount(recommend.getRecommendWalletAddress(), dayTime);

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
            return SingleResponse.buildFailure("操作被中断");
        } catch (Exception e) {
            return SingleResponse.buildFailure("删除挂单失败: " + e.getMessage());
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
        if(Objects.nonNull(recommend)) {
            // 有上级，更新上级的推荐人总算力
            RecommendStatisticsLog parentLog = getOrCreateAccount(recommend.getRecommendWalletAddress(), dayTime);

            BigDecimal parentTotalRecommendComputingPower = new BigDecimal(parentLog.getTotalRecommendComputingPower()).add(computingPower);

            parentLog.setTotalRecommendComputingPower(parentTotalRecommendComputingPower.toString());

            recommendStatisticsLogMapper.updateById(parentLog);

            if (Objects.isNull(recommend.getRecommendWalletAddress())) {
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


        return SingleResponse.of(recommendStatisticsLogDTO);

    }

    @Override
    public MultiResponse<RecommendStatisticsLogDTO> list(RecommendStatisticsLogListQry recommendStatisticsLogListQry) {



        LambdaQueryWrapper<RecommendStatisticsLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StringUtils.hasLength(recommendStatisticsLogListQry.getWalletAddress()), RecommendStatisticsLog::getWalletAddress, recommendStatisticsLogListQry.getWalletAddress());

        List<RecommendStatisticsLog> recommendStatisticsLogs = recommendStatisticsLogMapper.selectList(queryWrapper);

        Map<String, List<RecommendStatisticsLog>> recommendStatisticsLogMap = recommendStatisticsLogs.stream().collect(Collectors.groupingBy(RecommendStatisticsLog::getWalletAddress));

        List<RecommendStatisticsLogDTO> recommendStatisticsLogDTOS = new ArrayList<>();

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



            RecommendStatisticsLogDTO recommendStatisticsLogDTO = new RecommendStatisticsLogDTO();
            recommendStatisticsLogDTO.setWalletAddress(walletAddress);
            recommendStatisticsLogDTO.setDirectRecommendCount(directRecommendCount);
            recommendStatisticsLogDTO.setTotalComputingPower(totalComputingPower.toString());
            recommendStatisticsLogDTO.setTotalRecommendComputingPower(totalRecommendComputingPower.toString());
            recommendStatisticsLogDTO.setTotalDirectRecommendComputingPower(totalDirectRecommendComputingPower.toString());


            recommendStatisticsLogDTOS.add(recommendStatisticsLogDTO);
        }


        return MultiResponse.of(recommendStatisticsLogDTOS);
    }




    /**
     * 获取或创建统计记录
     */
    public RecommendStatisticsLog getOrCreateAccount(String walletAddress, String dayTime){

        LambdaQueryWrapper<RecommendStatisticsLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RecommendStatisticsLog::getWalletAddress, walletAddress);
        queryWrapper.eq(RecommendStatisticsLog::getDayTime, dayTime);

        RecommendStatisticsLog existingLog = recommendStatisticsLogMapper.selectOne(queryWrapper);
        if (Objects.isNull(existingLog)){

            existingLog = new RecommendStatisticsLog();
            existingLog.setWalletAddress(walletAddress);
            existingLog.setDayTime(dayTime);
            existingLog.setDirectRecommendCount(0);
            existingLog.setTotalComputingPower("0");
            existingLog.setTotalRecommendComputingPower("0");
            existingLog.setTotalDirectRecommendComputingPower("0");

            recommendStatisticsLogMapper.insert(existingLog);
        }

        return existingLog;
    }


}
