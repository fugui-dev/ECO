package com.example.eco.util;

import com.example.eco.model.entity.MinerProject;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * ESG购买限制工具类
 * 用于管理ESG矿机每日投放限制和用户每日购买限制
 * 基于MinerProject的esgRushMode和esgRushLimit字段控制
 */
@Slf4j
@Component
public class EsgPurchaseLimitUtil {

    @Resource
    private RedissonClient redissonClient;

    // Redis key前缀
    private static final String ESG_DAILY_COUNT_PREFIX = "esg:daily:count:";

    private static final String ESG_USER_DAILY_PREFIX = "esg:user:daily:";

    private static final String ESG_PURCHASE_LOCK_PREFIX = "esg:purchase:lock:";

    /**
     * 检查ESG每日投放是否还有剩余（仅用于快速检查，不保证并发安全）
     * @param minerProject 矿机项目
     * @return true-还有剩余，false-已达上限
     */
    public boolean checkEsgDailyLimit(MinerProject minerProject) {
        // 如果未开启ESG抢购模式，不限制数量
        if (minerProject.getEsgRushMode() == null || minerProject.getEsgRushMode() != 1) {
            log.debug("ESG抢购模式未开启 - 矿机ID: {}, 不限制数量", minerProject.getId());
            return true;
        }
        
        // 如果开启抢购模式但未设置限制数量，使用默认值
        int rushLimit = minerProject.getEsgRushLimit() != null ? minerProject.getEsgRushLimit() : 50;
        
        String dayTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String countKey = ESG_DAILY_COUNT_PREFIX + dayTime + ":" + minerProject.getId();
        
        RAtomicLong dailyCount = redissonClient.getAtomicLong(countKey);

        long currentCount = dailyCount.get();
        
        log.debug("ESG每日限额检查 - 矿机ID: {}, 当前数量: {}, 限制数量: {}, 抢购模式: {}", 
                minerProject.getId(), currentCount, rushLimit, minerProject.getEsgRushMode());
        
        return currentCount < rushLimit;
    }

    /**
     * 原子性检查和增加ESG购买计数（并发安全）
     * @param minerProject 矿机项目
     * @param walletAddress 钱包地址
     * @return true-成功购买，false-已达上限或已购买
     */
    public boolean tryPurchaseEsg(MinerProject minerProject, String walletAddress) {
        // 如果未开启ESG抢购模式，直接返回成功
        if (minerProject.getEsgRushMode() == null || minerProject.getEsgRushMode() != 1) {
            log.debug("ESG抢购模式未开启 - 矿机ID: {}, 直接返回成功", minerProject.getId());
            return true;
        }
        
        // 如果开启抢购模式但未设置限制数量，使用默认值
        int rushLimit = minerProject.getEsgRushLimit() != null ? minerProject.getEsgRushLimit() : 50;
        
        String dayTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String countKey = ESG_DAILY_COUNT_PREFIX + dayTime + ":" + minerProject.getId();

        String userKey = ESG_USER_DAILY_PREFIX + dayTime + ":" + walletAddress + ":" + minerProject.getId();
        
        RAtomicLong dailyCount = redissonClient.getAtomicLong(countKey);
        
        // 使用分布式锁确保原子性
        String lockKey = ESG_PURCHASE_LOCK_PREFIX + minerProject.getId();

        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                // 1. 检查用户今日是否已购买
                if (redissonClient.getBucket(userKey).isExists()) {
                    log.warn("用户今日已购买ESG矿机 - 矿机ID: {}, 钱包地址: {}", minerProject.getId(), walletAddress);
                    return false;
                }
                
                // 2. 检查每日限额
                long currentCount = dailyCount.get();

                if (currentCount >= rushLimit) {
                    log.warn("ESG每日限额已达上限 - 矿机ID: {}, 当前数量: {}, 限制数量: {}", minerProject.getId(), currentCount, rushLimit);
                    return false;
                }
                
                // 3. 原子性增加计数
                long newCount = dailyCount.incrementAndGet();
                
                // 4. 标记用户购买记录
                redissonClient.getBucket(userKey).set("1", 2, TimeUnit.DAYS);
                
                // 5. 设置过期时间
                dailyCount.expire(2, TimeUnit.DAYS);
                
                log.info("ESG购买成功 - 矿机ID: {}, 钱包地址: {}, 新数量: {}, 限制数量: {}", minerProject.getId(), walletAddress, newCount, rushLimit);
                
                return true;
            } else {
                log.warn("获取ESG购买锁失败 - 矿机ID: {}", minerProject.getId());
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("ESG购买锁被中断 - 矿机ID: {}", minerProject.getId(), e);
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 增加ESG每日购买计数
     * @param minerProject 矿机项目
     * @return true-成功增加，false-已达上限
     */
    public boolean incrementEsgDailyCount(MinerProject minerProject) {
        // 如果未开启ESG抢购模式，直接返回成功
        if (minerProject.getEsgRushMode() == null || minerProject.getEsgRushMode() != 1) {
            log.debug("ESG抢购模式未开启 - 矿机ID: {}, 直接返回成功", minerProject.getId());
            return true;
        }
        
        // 如果开启抢购模式但未设置限制数量，使用默认值
        int rushLimit = minerProject.getEsgRushLimit() != null ? minerProject.getEsgRushLimit() : 50;
        
        String dayTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String countKey = ESG_DAILY_COUNT_PREFIX + dayTime + ":" + minerProject.getId();
        
        RAtomicLong dailyCount = redissonClient.getAtomicLong(countKey);
        
        // 使用分布式锁确保原子性
        String lockKey = ESG_PURCHASE_LOCK_PREFIX + minerProject.getId();
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                long currentCount = dailyCount.get();
                
                if (currentCount >= rushLimit) {
                    log.warn("ESG每日限额已达上限 - 矿机ID: {}, 当前数量: {}, 限制数量: {}", 
                            minerProject.getId(), currentCount, rushLimit);
                    return false;
                }
                
                long newCount = dailyCount.incrementAndGet();
                log.info("ESG每日购买计数增加 - 矿机ID: {}, 新数量: {}, 限制数量: {}", 
                        minerProject.getId(), newCount, rushLimit);
                
                // 设置过期时间为2天，避免数据积累
                dailyCount.expire(2, TimeUnit.DAYS);
                
                return true;
            } else {
                log.warn("获取ESG购买锁失败 - 矿机ID: {}", minerProject.getId());
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("ESG购买锁被中断 - 矿机ID: {}", minerProject.getId(), e);
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 检查用户今日是否已购买ESG矿机
     * @param walletAddress 钱包地址
     * @param minerProjectId 矿机项目ID
     * @return true-已购买，false-未购买
     */
    public boolean checkUserDailyEsgPurchase(String walletAddress, Integer minerProjectId) {
        String dayTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String userKey = ESG_USER_DAILY_PREFIX + dayTime + ":" + walletAddress + ":" + minerProjectId;
        
        return redissonClient.getBucket(userKey).isExists();
    }

    /**
     * 标记用户今日已购买ESG矿机
     * @param walletAddress 钱包地址
     * @param minerProjectId 矿机项目ID
     */
    public void markUserDailyEsgPurchase(String walletAddress, Integer minerProjectId) {
        String dayTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String userKey = ESG_USER_DAILY_PREFIX + dayTime + ":" + walletAddress + ":" + minerProjectId;
        
        redissonClient.getBucket(userKey).set("1", 2, TimeUnit.DAYS);
        log.info("标记用户ESG购买记录 - 钱包地址: {}, 矿机ID: {}, 日期: {}", 
                walletAddress, minerProjectId, dayTime);
    }

    /**
     * 获取今日ESG购买数量
     * @param minerProjectId 矿机项目ID
     * @return 今日购买数量
     */
    public long getTodayEsgPurchaseCount(Integer minerProjectId) {
        String dayTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String countKey = ESG_DAILY_COUNT_PREFIX + dayTime + ":" + minerProjectId;
        
        RAtomicLong dailyCount = redissonClient.getAtomicLong(countKey);
        return dailyCount.get();
    }

    /**
     * 重置今日ESG购买计数（用于测试或特殊情况）
     * @param minerProjectId 矿机项目ID
     */
    public void resetTodayEsgPurchaseCount(Integer minerProjectId) {
        String dayTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String countKey = ESG_DAILY_COUNT_PREFIX + dayTime + ":" + minerProjectId;
        
        RAtomicLong dailyCount = redissonClient.getAtomicLong(countKey);
        dailyCount.delete();
        log.info("重置ESG购买计数 - 矿机ID: {}, 日期: {}", minerProjectId, dayTime);
    }

    /**
     * 检查矿机是否开启ESG抢购模式
     * @param minerProject 矿机项目
     * @return true-开启抢购模式，false-未开启
     */
    public boolean isEsgRushModeEnabled(MinerProject minerProject) {

        return minerProject.getEsgRushMode() != null && minerProject.getEsgRushMode() == 1;
    }

    /**
     * 获取矿机ESG抢购限制数量
     * @param minerProject 矿机项目
     * @return 抢购限制数量，如果未开启抢购模式返回-1
     */
    public int getEsgRushLimit(MinerProject minerProject) {
        if (!isEsgRushModeEnabled(minerProject)) {
            return -1; // 未开启抢购模式
        }
        return minerProject.getEsgRushLimit() != null ? minerProject.getEsgRushLimit() : 50;
    }

    /**
     * 回滚ESG购买计数（用于扣款失败时）
     * @param minerProject 矿机项目
     * @param walletAddress 钱包地址
     * @return 是否回滚成功
     */
    public boolean rollbackEsgPurchase(MinerProject minerProject, String walletAddress) {
        // 如果未开启ESG抢购模式，无需回滚
        if (minerProject.getEsgRushMode() == null || minerProject.getEsgRushMode() != 1) {
            return true;
        }
        
        String dayTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String countKey = ESG_DAILY_COUNT_PREFIX + dayTime + ":" + minerProject.getId();
        String userKey = ESG_USER_DAILY_PREFIX + dayTime + ":" + walletAddress + ":" + minerProject.getId();
        
        String lockKey = ESG_PURCHASE_LOCK_PREFIX + minerProject.getId();
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                // 减少计数
                RAtomicLong dailyCount = redissonClient.getAtomicLong(countKey);
                long currentCount = dailyCount.get();
                if (currentCount > 0) {
                    dailyCount.decrementAndGet();
                }
                
                // 删除用户购买记录
                redissonClient.getBucket(userKey).delete();
                
                log.info("ESG购买回滚成功 - 矿机ID: {}, 钱包地址: {}, 当前数量: {}", 
                        minerProject.getId(), walletAddress, dailyCount.get());
                
                return true;
            } else {
                log.warn("获取ESG回滚锁失败 - 矿机ID: {}", minerProject.getId());
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("ESG回滚锁被中断 - 矿机ID: {}", minerProject.getId(), e);
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 获取ESG购买统计信息
     * @param minerProject 矿机项目
     * @return 统计信息字符串
     */
    public String getEsgPurchaseStats(MinerProject minerProject) {
        if (!isEsgRushModeEnabled(minerProject)) {
            return String.format("ESG购买统计 - 矿机ID: %d, 抢购模式: 未开启, 无数量限制", 
                    minerProject.getId());
        }
        
        int rushLimit = getEsgRushLimit(minerProject);
        long todayCount = getTodayEsgPurchaseCount(minerProject.getId());
        long remaining = rushLimit - todayCount;
        boolean available = remaining > 0;
        
        return String.format("ESG购买统计 - 矿机ID: %d, 抢购模式: 已开启, 每日限制: %d台, 今日已售: %d台, 剩余: %d台, 可用: %s", 
                minerProject.getId(), rushLimit, todayCount, remaining, available ? "是" : "否");
    }
}