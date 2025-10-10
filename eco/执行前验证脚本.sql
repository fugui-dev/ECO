-- 执行回滚前的验证脚本
-- 请先运行此脚本确认数据状态

-- ==========================================
-- 第一步：验证目标地址数量
-- ==========================================

SELECT 
    '目标地址总数' AS description,
    COUNT(*) AS count
FROM (
    SELECT '0x8ee830de0cd81de591de840518e0f87902d01468' AS wallet_address
    UNION ALL SELECT '0x61809676ef8f5e4bad14514aee2dd0c9fd1f0c94'
    UNION ALL SELECT '0x925e88137dbd08cd0e9ac67c829ec505a1a597c6'
    UNION ALL SELECT '0x95066f226ce7e00c28acff0a34881b5dc18a69ce'
    UNION ALL SELECT '0x07d98e967ed89402c33291222438d2435a9229f5'
    UNION ALL SELECT '0x9a9e174f1d95d6fac5e1a9661a5447353a53ec7b'
    UNION ALL SELECT '0x4e4c61dcf2629a2186e1cb63ae8d96c93df695f0'
    UNION ALL SELECT '0x0186f88fc78327927d31f126f0c1b09e3092c2c4'
    UNION ALL SELECT '0x585bd44e61cc21ad75f37d42710f8fcda213ff29'
    UNION ALL SELECT '0xad6f3a147f779f513d99d81bfdfb5ccae56e419a'
    -- ... 其他地址
) AS target_addresses;

-- ==========================================
-- 第二步：检查这些地址是否有新增算力奖励
-- ==========================================

SELECT 
    '2025-10-09新增算力奖励统计' AS description,
    COUNT(*) AS reward_count,
    SUM(CAST(reward AS DECIMAL(20,8))) AS total_reward_amount,
    SUM(CAST(reward_price AS DECIMAL(20,8))) AS total_reward_price_amount
FROM purchase_miner_project_reward 
WHERE wallet_address IN (
    '0x8ee830de0cd81de591de840518e0f87902d01468',
    '0x61809676ef8f5e4bad14514aee2dd0c9fd1f0c94',
    '0x925e88137dbd08cd0e9ac67c829ec505a1a597c6',
    '0x95066f226ce7e00c28acff0a34881b5dc18a69ce',
    '0x07d98e967ed89402c33291222438d2435a9229f5',
    '0x9a9e174f1d95d6fac5e1a9661a5447353a53ec7b',
    '0x4e4c61dcf2629a2186e1cb63ae8d96c93df695f0',
    '0x0186f88fc78327927d31f126f0c1b09e3092c2c4',
    '0x585bd44e61cc21ad75f37d42710f8fcda213ff29',
    '0xad6f3a147f779f513d99d81bfdfb5ccae56e419a'
    -- 添加更多地址...
) AND day_time = '2025-10-09'
AND reward_type = 'NEW';

-- ==========================================
-- 第三步：检查这些地址的账户余额
-- ==========================================

SELECT 
    '目标地址账户余额统计' AS description,
    COUNT(*) AS account_count,
    SUM(CAST(number AS DECIMAL(20,8))) AS total_balance,
    SUM(CAST(dynamic_reward AS DECIMAL(20,8))) AS total_dynamic_reward,
    SUM(CAST(dynamic_reward_price AS DECIMAL(20,8))) AS total_dynamic_reward_price
FROM account 
WHERE wallet_address IN (
    '0x8ee830de0cd81de591de840518e0f87902d01468',
    '0x61809676ef8f5e4bad14514aee2dd0c9fd1f0c94',
    '0x925e88137dbd08cd0e9ac67c829ec505a1a597c6',
    '0x95066f226ce7e00c28acff0a34881b5dc18a69ce',
    '0x07d98e967ed89402c33291222438d2435a9229f5',
    '0x9a9e174f1d95d6fac5e1a9661a5447353a53ec7b',
    '0x4e4c61dcf2629a2186e1cb63ae8d96c93df695f0',
    '0x0186f88fc78327927d31f126f0c1b09e3092c2c4',
    '0x585bd44e61cc21ad75f37d42710f8fcda213ff29',
    '0xad6f3a147f779f513d99d81bfdfb5ccae56e419a'
    -- 添加更多地址...
) AND type = 'ECO';

-- ==========================================
-- 第四步：检查这些地址的矿机奖励数据
-- ==========================================

SELECT 
    '目标地址矿机奖励统计' AS description,
    COUNT(*) AS miner_count,
    SUM(CAST(reward AS DECIMAL(20,8))) AS total_miner_reward,
    SUM(CAST(reward_price AS DECIMAL(20,8))) AS total_miner_reward_price
FROM purchase_miner_project 
WHERE wallet_address IN (
    '0x8ee830de0cd81de591de840518e0f87902d01468',
    '0x61809676ef8f5e4bad14514aee2dd0c9fd1f0c94',
    '0x925e88137dbd08cd0e9ac67c829ec505a1a597c6',
    '0x95066f226ce7e00c28acff0a34881b5dc18a69ce',
    '0x07d98e967ed89402c33291222438d2435a9229f5',
    '0x9a9e174f1d95d6fac5e1a9661a5447353a53ec7b',
    '0x4e4c61dcf2629a2186e1cb63ae8d96c93df695f0',
    '0x0186f88fc78327927d31f126f0c1b09e3092c2c4',
    '0x585bd44e61cc21ad75f37d42710f8fcda213ff29',
    '0xad6f3a147f779f513d99d81bfdfb5ccae56e419a'
    -- 添加更多地址...
) AND status = 'SUCCESS';

-- ==========================================
-- 第五步：检查这些地址的交易记录
-- ==========================================

SELECT 
    '目标地址交易记录统计' AS description,
    COUNT(*) AS transaction_count,
    transaction_type,
    SUM(CAST(number AS DECIMAL(20,8))) AS total_amount
FROM account_transaction 
WHERE wallet_address IN (
    '0x8ee830de0cd81de591de840518e0f87902d01468',
    '0x61809676ef8f5e4bad14514aee2dd0c9fd1f0c94',
    '0x925e88137dbd08cd0e9ac67c829ec505a1a597c6',
    '0x95066f226ce7e00c28acff0a34881b5dc18a69ce',
    '0x07d98e967ed89402c33291222438d2435a9229f5',
    '0x9a9e174f1d95d6fac5e1a9661a5447353a53ec7b',
    '0x4e4c61dcf2629a2186e1cb63ae8d96c93df695f0',
    '0x0186f88fc78327927d31f126f0c1b09e3092c2c4',
    '0x585bd44e61cc21ad75f37d42710f8fcda213ff29',
    '0xad6f3a147f779f513d99d81bfdfb5ccae56e419a'
    -- 添加更多地址...
) AND transaction_type IN ('DYNAMIC_REWARD', 'ADD_NUMBER')
GROUP BY transaction_type;

-- ==========================================
-- 第六步：检查这些地址的推荐关系
-- ==========================================

SELECT 
    '目标地址推荐关系检查' AS description,
    COUNT(*) AS has_recommended_count
FROM recommend 
WHERE recommend_wallet_address IN (
    '0x8ee830de0cd81de591de840518e0f87902d01468',
    '0x61809676ef8f5e4bad14514aee2dd0c9fd1f0c94',
    '0x925e88137dbd08cd0e9ac67c829ec505a1a597c6',
    '0x95066f226ce7e00c28acff0a34881b5dc18a69ce',
    '0x07d98e967ed89402c33291222438d2435a9229f5',
    '0x9a9e174f1d95d6fac5e1a9661a5447353a53ec7b',
    '0x4e4c61dcf2629a2186e1cb63ae8d96c93df695f0',
    '0x0186f88fc78327927d31f126f0c1b09e3092c2c4',
    '0x585bd44e61cc21ad75f37d42710f8fcda213ff29',
    '0xad6f3a147f779f513d99d81bfdfb5ccae56e419a'
    -- 添加更多地址...
);

-- ==========================================
-- 第七步：详细查看前10个地址的奖励记录
-- ==========================================

SELECT 
    wallet_address,
    reward,
    reward_price,
    computing_power,
    min_computing_power,
    total_computing_power,
    create_time
FROM purchase_miner_project_reward 
WHERE wallet_address IN (
    '0x8ee830de0cd81de591de840518e0f87902d01468',
    '0x61809676ef8f5e4bad14514aee2dd0c9fd1f0c94',
    '0x925e88137dbd08cd0e9ac67c829ec505a1a597c6',
    '0x95066f226ce7e00c28acff0a34881b5dc18a69ce',
    '0x07d98e967ed89402c33291222438d2435a9229f5',
    '0x9a9e174f1d95d6fac5e1a9661a5447353a53ec7b',
    '0x4e4c61dcf2629a2186e1cb63ae8d96c93df695f0',
    '0x0186f88fc78327927d31f126f0c1b09e3092c2c4',
    '0x585bd44e61cc21ad75f37d42710f8fcda213ff29',
    '0xad6f3a147f779f513d99d81bfdfb5ccae56e419a'
) AND day_time = '2025-10-09'
AND reward_type = 'NEW'
ORDER BY CAST(reward AS DECIMAL(20,8)) DESC
LIMIT 10;
