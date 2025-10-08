package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.bean.dto.RecommendDTO;
import com.example.eco.bean.dto.RecommendRecordDTO;
import com.example.eco.common.AccountType;
import com.example.eco.common.RecommendStatus;
import com.example.eco.core.service.AccountService;
import com.example.eco.core.service.RecommendService;
import com.example.eco.core.service.RecommendStatisticsLogService;
import com.example.eco.core.service.impl.ComputingPowerServiceImplV2;
import com.example.eco.model.entity.Account;
import com.example.eco.model.entity.Recommend;
import com.example.eco.model.entity.RecommendRecord;
import com.example.eco.model.entity.RecommendStatisticsLog;
import com.example.eco.model.mapper.AccountMapper;
import com.example.eco.model.mapper.RecommendMapper;
import com.example.eco.model.mapper.RecommendRecordMapper;
import com.example.eco.model.mapper.RecommendStatisticsLogMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class RecommendServiceImpl implements RecommendService {

    @Resource
    private RecommendMapper recommendMapper;
    @Resource
    private RecommendRecordMapper recommendRecordMapper;
    @Resource
    private AccountService accountService;
    @Resource
    private RecommendStatisticsLogService recommendStatisticsLogService;
    @Resource
    private ComputingPowerServiceImplV2 computingPowerServiceV2;
    @Resource
    private RecommendStatisticsLogMapper recommendStatisticsLogMapper;
    @Resource
    private RedissonClient redissonClient;

    @Override
    public SingleResponse<RecommendDTO> get(RecommendQry recommendQry) {

        LambdaQueryWrapper<Recommend> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Recommend::getWalletAddress, recommendQry.getWalletAddress());

        Recommend recommend = recommendMapper.selectOne(lambdaQueryWrapper);
        if (Objects.isNull(recommend)) {
            //不存在就创建
            RecommendCreateCmd recommendCreateCmd = new RecommendCreateCmd();
            recommendCreateCmd.setWalletAddress(recommendQry.getWalletAddress());
            recommend = create(recommendCreateCmd);
        }

        if (Objects.isNull(recommend.getRecommendCode())){
            recommend.setRecommendCode(getRecommendCode());
            recommendMapper.updateById(recommend);
        }

        RecommendDTO recommendDTO = new RecommendDTO();
        BeanUtils.copyProperties(recommend, recommendDTO);
        return SingleResponse.of(recommendDTO);
    }

    @Override
    public MultiResponse<RecommendRecordDTO> page(RecommendPageQry recommendPageQry) {

        LambdaQueryWrapper<RecommendRecord> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(RecommendRecord::getRecommendWalletAddress, recommendPageQry.getWalletAddress());

        Page<RecommendRecord> recommendRecordPage = recommendRecordMapper.selectPage(Page.of(recommendPageQry.getPageNum(), recommendPageQry.getPageSize()), lambdaQueryWrapper);
        if (CollectionUtils.isEmpty(recommendRecordPage.getRecords())) {
            return MultiResponse.buildSuccess();
        }

        List<RecommendRecordDTO> recommendRecordDTOList = new ArrayList<>();
        for (RecommendRecord recommendRecord : recommendRecordPage.getRecords()) {
            RecommendRecordDTO recommendRecordDTO = new RecommendRecordDTO();
            BeanUtils.copyProperties(recommendRecord, recommendRecordDTO);
            recommendRecordDTOList.add(recommendRecordDTO);
        }


        return MultiResponse.of(recommendRecordDTOList, (int) recommendRecordPage.getTotal());
    }

    @Transactional
    @Override
    public SingleResponse<Void> recommend(RecommendCreateCmd recommendCreateCmd) {
        // 使用推荐码作为锁的key
        String lockKey = "recommend:code:" + recommendCreateCmd.getRecommendCode();
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // 尝试获取锁，最多等待10秒，锁持有时间最多30秒
            boolean acquired = lock.tryLock(10, 30, java.util.concurrent.TimeUnit.SECONDS);
            if (!acquired) {
                return SingleResponse.buildFailure("系统繁忙，请稍后重试");
            }

            LambdaQueryWrapper<Recommend> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(Recommend::getWalletAddress, recommendCreateCmd.getRecommendWalletAddress());

            //查询被推荐人信息
            Recommend recommended = recommendMapper.selectOne(lambdaQueryWrapper);
            if (Objects.isNull(recommended)) {
                //不存在就创建
                RecommendCreateCmd recommendedCreateCmd = new RecommendCreateCmd();
                recommendedCreateCmd.setWalletAddress(recommendCreateCmd.getRecommendWalletAddress());
                recommended = create(recommendedCreateCmd);
            }

            if (Objects.nonNull(recommended.getRecommendWalletAddress())) {
                return SingleResponse.buildFailure("该钱包已被推荐，不能重复推荐");
            }

            LambdaQueryWrapper<Recommend> recommendLambdaQueryWrapper = new LambdaQueryWrapper<>();
            recommendLambdaQueryWrapper.eq(Recommend::getRecommendCode, recommendCreateCmd.getRecommendCode());
            //查询推荐人信息 推荐人必须存在
            Recommend recommender = recommendMapper.selectOne(recommendLambdaQueryWrapper);
            if (Objects.isNull(recommender)) {
               return SingleResponse.buildFailure("推荐码不存在");
            }

            if (recommender.getWalletAddress().equals(recommended.getWalletAddress())){
                return SingleResponse.buildFailure("不能推荐自己");
            }

            if (Objects.nonNull(recommender.getLeaderWalletAddress()) &&
                    Objects.nonNull(recommended.getLeaderWalletAddress()) &&
                    recommender.getLeaderWalletAddress().equals(recommended.getLeaderWalletAddress())){
                return SingleResponse.buildFailure("不能推荐同队的");
            }

            String leaderRecommender = recommender.getLeaderWalletAddress();

            recommended.setLevel(recommender.getLevel() + 1);
            recommended.setRecommendWalletAddress(recommender.getWalletAddress());
            recommended.setLeaderWalletAddress(leaderRecommender);
            recommended.setUpdateTime(System.currentTimeMillis());
            recommendMapper.updateById(recommended);

            RecommendRecord recommendRecord = new RecommendRecord();
            recommendRecord.setWalletAddress(recommendCreateCmd.getRecommendWalletAddress());
            recommendRecord.setRecommendWalletAddress(recommender.getWalletAddress());
            recommendRecord.setRecommendTime(System.currentTimeMillis());

            recommendRecordMapper.insert(recommendRecord);

        // 推荐关系建立成功，清除推荐人算力缓存，让下次查询时重新计算
        computingPowerServiceV2.invalidateUserCache(recommender.getWalletAddress());

            //更新被推荐者的下级leader
            LambdaQueryWrapper<Recommend> leaderLambdaQueryWrapper = new LambdaQueryWrapper<>();
            leaderLambdaQueryWrapper.eq(Recommend::getLeaderWalletAddress, recommendCreateCmd.getRecommendWalletAddress());

            List<Recommend> recommendList = recommendMapper.selectList(leaderLambdaQueryWrapper);
            for (Recommend recommend : recommendList) {
                recommend.setLevel(recommend.getLevel() + 1);
                recommend.setLeaderWalletAddress(leaderRecommender);
                recommend.setUpdateTime(System.currentTimeMillis());

                recommendMapper.updateById(recommend);
            }

            return SingleResponse.buildSuccess();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SingleResponse.buildFailure("操作被中断，请重试");
        } catch (Exception e) {
            return SingleResponse.buildFailure("推荐操作失败：" + e.getMessage());
        } finally {
            // 确保锁被释放
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 创建推荐关系
     */
    public Recommend create(RecommendCreateCmd recommendCreateCmd) {
        // 使用钱包地址作为锁的key，确保同一钱包地址的创建操作串行化
        String lockKey = "recommend:create:" + recommendCreateCmd.getWalletAddress();
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // 尝试获取锁，最多等待10秒，锁持有时间最多30秒
            boolean acquired = lock.tryLock(10, 30, java.util.concurrent.TimeUnit.SECONDS);
            if (!acquired) {
                throw new RuntimeException("获取分布式锁失败，请稍后重试");
            }
            
            // 双重检查，防止重复创建
            LambdaQueryWrapper<Recommend> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(Recommend::getWalletAddress, recommendCreateCmd.getWalletAddress());

            Recommend recommend = recommendMapper.selectOne(lambdaQueryWrapper);
            if (Objects.nonNull(recommend)) {
                return recommend;
            }

            recommend = new Recommend();
            recommend.setWalletAddress(recommendCreateCmd.getWalletAddress());
            recommend.setStatus(RecommendStatus.NORMAL.getCode());
            recommend.setLevel(0);
            recommend.setLeaderWalletAddress(recommendCreateCmd.getWalletAddress());
            recommend.setCreateTime(System.currentTimeMillis());
            recommend.setRecommendCode(getRecommendCode());
            recommendMapper.insert(recommend);

            AccountCreateCmd accountCreateCmd = new AccountCreateCmd();
            accountCreateCmd.setWalletAddress(recommendCreateCmd.getWalletAddress());

            accountService.createAccount(accountCreateCmd);

            return recommend;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取分布式锁被中断", e);
        } catch (Exception e) {
            throw new RuntimeException("创建推荐关系失败", e);
        } finally {
            // 确保锁被释放
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 获取推荐码
     */
    public synchronized String getRecommendCode(){

        String randomAlphabetic;

        while (true) {

            randomAlphabetic = RandomStringUtils.random(7, true, true).toUpperCase();

            LambdaQueryWrapper<Recommend> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(Recommend::getRecommendCode,randomAlphabetic);

            Long count = recommendMapper.selectCount(lambdaQueryWrapper);
            if (count == 0) {
                break;
            }
        }

        return randomAlphabetic;
    }
}
