-- 回滚新增算力奖励数据的完整SQL脚本
-- 请根据查询结果替换具体的钱包地址和日期

-- ==========================================
-- 第一步：备份数据（重要！）
-- ==========================================

-- 备份账户数据
CREATE TABLE backup_account_before_rollback AS 
SELECT * FROM account 
WHERE wallet_address IN (
    '0x1234...',  -- 替换为查询出的钱包地址
    '0x5678...',
    '0x9abc...'
    -- 添加更多地址...
);

-- 备份奖励记录
CREATE TABLE backup_purchase_miner_project_reward_before_rollback AS 
SELECT * FROM purchase_miner_project_reward 
WHERE wallet_address IN (
    '0x1234...',  -- 替换为查询出的钱包地址
    '0x5678...',
    '0x9abc...'
    -- 添加更多地址...
) AND day_time = '2024-01-15'  -- 替换为具体日期
AND reward_type = 'NEW';

-- 备份账户交易记录
CREATE TABLE backup_account_transaction_before_rollback AS 
SELECT * FROM account_transaction 
WHERE wallet_address IN (
    '0x1234...',  -- 替换为查询出的钱包地址
    '0x5678...',
    '0x9abc...'
    -- 添加更多地址...
) AND transaction_type IN ('DYNAMIC_REWARD', 'ADD_NUMBER');

-- ==========================================
-- 第二步：计算回滚金额
-- ==========================================

-- 创建临时表存储回滚金额
CREATE TEMPORARY TABLE rollback_amounts AS
SELECT 
    wallet_address,
    SUM(CAST(reward AS DECIMAL(20,8))) AS total_reward,
    SUM(CAST(reward_price AS DECIMAL(20,8))) AS total_reward_price
FROM purchase_miner_project_reward 
WHERE wallet_address IN (
    '0x1234...',  -- 替换为查询出的钱包地址
    '0x5678...',
    '0x9abc...'
    -- 添加更多地址...
) AND day_time = '2024-01-15'  -- 替换为具体日期
AND reward_type = 'NEW'
GROUP BY wallet_address;

-- ==========================================
-- 第三步：回滚账户余额
-- ==========================================

-- 回滚账户的number字段（总余额）
UPDATE account a
SET number = CAST(number AS DECIMAL(20,8)) - ra.total_reward,
    update_time = UNIX_TIMESTAMP() * 1000
FROM rollback_amounts ra
WHERE a.wallet_address = ra.wallet_address
  AND a.type = 'ECO';

-- 回滚账户的dynamic_reward字段（动态奖励）
UPDATE account a
SET dynamic_reward = CAST(dynamic_reward AS DECIMAL(20,8)) - ra.total_reward,
    update_time = UNIX_TIMESTAMP() * 1000
FROM rollback_amounts ra
WHERE a.wallet_address = ra.wallet_address
  AND a.type = 'ECO';

-- 回滚账户的dynamic_reward_price字段（动态奖励价值）
UPDATE account a
SET dynamic_reward_price = CAST(dynamic_reward_price AS DECIMAL(20,8)) - ra.total_reward_price,
    update_time = UNIX_TIMESTAMP() * 1000
FROM rollback_amounts ra
WHERE a.wallet_address = ra.wallet_address
  AND a.type = 'ECO';

-- ==========================================
-- 第四步：删除账户交易记录
-- ==========================================

-- 删除动态奖励相关的交易记录
DELETE FROM account_transaction 
WHERE wallet_address IN (
    '0x1234...',  -- 替换为查询出的钱包地址
    '0x5678...',
    '0x9abc...'
    -- 添加更多地址...
) AND transaction_type = 'DYNAMIC_REWARD'
AND `order` IN (
    SELECT DISTINCT `order` 
    FROM purchase_miner_project_reward 
    WHERE wallet_address IN (
        '0x1234...',  -- 替换为查询出的钱包地址
        '0x5678...',
        '0x9abc...'
        -- 添加更多地址...
    ) AND day_time = '2024-01-15'  -- 替换为具体日期
    AND reward_type = 'NEW'
);

-- 删除增加余额相关的交易记录
DELETE FROM account_transaction 
WHERE wallet_address IN (
    '0x1234...',  -- 替换为查询出的钱包地址
    '0x5678...',
    '0x9abc...'
    -- 添加更多地址...
) AND transaction_type = 'ADD_NUMBER'
AND `order` IN (
    SELECT DISTINCT `order` 
    FROM purchase_miner_project_reward 
    WHERE wallet_address IN (
        '0x1234...',  -- 替换为查询出的钱包地址
        '0x5678...',
        '0x9abc...'
        -- 添加更多地址...
    ) AND day_time = '2024-01-15'  -- 替换为具体日期
    AND reward_type = 'NEW'
);

-- ==========================================
-- 第五步：回滚矿机奖励数据
-- ==========================================

-- 回滚矿机的奖励数量
UPDATE purchase_miner_project pmp
SET reward = CAST(reward AS DECIMAL(20,8)) - COALESCE(ra.total_reward, 0),
    reward_price = CAST(reward_price AS DECIMAL(20,8)) - COALESCE(ra.total_reward_price, 0),
    update_time = UNIX_TIMESTAMP() * 1000
FROM rollback_amounts ra
WHERE pmp.wallet_address = ra.wallet_address
  AND pmp.status = 'SUCCESS';

-- ==========================================
-- 第六步：删除奖励记录
-- ==========================================

-- 删除新增算力奖励记录
DELETE FROM purchase_miner_project_reward 
WHERE wallet_address IN (
    '0x1234...',  -- 替换为查询出的钱包地址
    '0x5678...',
    '0x9abc...'
    -- 添加更多地址...
) AND day_time = '2024-01-15'  -- 替换为具体日期
AND reward_type = 'NEW';

-- ==========================================
-- 第七步：删除奖励日志记录
-- ==========================================

-- 删除reward_log表中的相关记录
DELETE FROM reward_log 
WHERE wallet_address IN (
    '0x1234...',  -- 替换为查询出的钱包地址
    '0x5678...',
    '0x9abc...'
    -- 添加更多地址...
) AND day_time = '2024-01-15'  -- 替换为具体日期
AND reward_type = 'NEW';

-- ==========================================
-- 第八步：验证回滚结果
-- ==========================================

-- 验证账户余额是否正确回滚
SELECT 
    wallet_address,
    number,
    dynamic_reward,
    dynamic_reward_price,
    update_time
FROM account 
WHERE wallet_address IN (
    '0x1234...',  -- 替换为查询出的钱包地址
    '0x5678...',
    '0x9abc...'
    -- 添加更多地址...
) AND type = 'ECO'
ORDER BY wallet_address;

-- 验证奖励记录是否已删除
SELECT COUNT(*) AS remaining_reward_records
FROM purchase_miner_project_reward 
WHERE wallet_address IN (
    '0x1234...',  -- 替换为查询出的钱包地址
    '0x5678...',
    '0x9abc...'
    -- 添加更多地址...
) AND day_time = '2024-01-15'  -- 替换为具体日期
AND reward_type = 'NEW';

-- 验证交易记录是否已删除
SELECT COUNT(*) AS remaining_transaction_records
FROM account_transaction 
WHERE wallet_address IN (
    '0x1234...',  -- 替换为查询出的钱包地址
    '0x5678...',
    '0x9abc...'
    -- 添加更多地址...
) AND transaction_type IN ('DYNAMIC_REWARD', 'ADD_NUMBER')
AND `order` IN (
    SELECT DISTINCT `order` 
    FROM backup_purchase_miner_project_reward_before_rollback
);

-- ==========================================
-- 第九步：清理临时表
-- ==========================================

DROP TEMPORARY TABLE rollback_amounts;

-- ==========================================
-- 回滚脚本（如果需要恢复数据）
-- ==========================================

/*
-- 如果需要回滚到执行前的状态，可以执行以下脚本：

-- 恢复账户数据
UPDATE account a
SET number = b.number,
    dynamic_reward = b.dynamic_reward,
    dynamic_reward_price = b.dynamic_reward_price,
    update_time = b.update_time
FROM backup_account_before_rollback b
WHERE a.wallet_address = b.wallet_address
  AND a.type = b.type;

-- 恢复奖励记录
INSERT INTO purchase_miner_project_reward 
SELECT * FROM backup_purchase_miner_project_reward_before_rollback;

-- 恢复交易记录
INSERT INTO account_transaction 
SELECT * FROM backup_account_transaction_before_rollback;

-- 恢复矿机奖励数据
UPDATE purchase_miner_project pmp
SET reward = b.reward,
    reward_price = b.reward_price,
    update_time = b.update_time
FROM backup_purchase_miner_project_reward_before_rollback b
WHERE pmp.wallet_address = b.wallet_address;
*/
