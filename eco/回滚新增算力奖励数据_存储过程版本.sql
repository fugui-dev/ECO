-- 回滚新增算力奖励数据的存储过程版本
-- 更安全，支持事务回滚

DELIMITER $$

-- 创建回滚存储过程
CREATE PROCEDURE RollbackNewPowerRewards(
    IN p_wallet_addresses TEXT,  -- 钱包地址列表，用逗号分隔
    IN p_day_time VARCHAR(10)    -- 日期，格式：yyyy-MM-dd
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    -- 创建临时表存储钱包地址
    CREATE TEMPORARY TABLE temp_wallet_addresses (
        wallet_address VARCHAR(255)
    );

    -- 解析钱包地址列表
    SET @sql = CONCAT('INSERT INTO temp_wallet_addresses VALUES ', 
                      REPLACE(REPLACE(p_wallet_addresses, '''', ''), ',', '),('), ')');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;

    -- 创建临时表存储回滚金额
    CREATE TEMPORARY TABLE temp_rollback_amounts AS
    SELECT 
        r.wallet_address,
        SUM(CAST(r.reward AS DECIMAL(20,8))) AS total_reward,
        SUM(CAST(r.reward_price AS DECIMAL(20,8))) AS total_reward_price
    FROM purchase_miner_project_reward r
    INNER JOIN temp_wallet_addresses t ON r.wallet_address = t.wallet_address
    WHERE r.day_time = p_day_time
      AND r.reward_type = 'NEW'
    GROUP BY r.wallet_address;

    -- 1. 回滚账户余额
    UPDATE account a
    INNER JOIN temp_rollback_amounts ra ON a.wallet_address = ra.wallet_address
    SET a.number = CAST(a.number AS DECIMAL(20,8)) - ra.total_reward,
        a.dynamic_reward = CAST(a.dynamic_reward AS DECIMAL(20,8)) - ra.total_reward,
        a.dynamic_reward_price = CAST(a.dynamic_reward_price AS DECIMAL(20,8)) - ra.total_reward_price,
        a.update_time = UNIX_TIMESTAMP() * 1000
    WHERE a.type = 'ECO';

    -- 2. 删除账户交易记录
    DELETE at FROM account_transaction at
    INNER JOIN temp_wallet_addresses t ON at.wallet_address = t.wallet_address
    WHERE at.transaction_type IN ('DYNAMIC_REWARD', 'ADD_NUMBER')
      AND at.`order` IN (
          SELECT DISTINCT `order` 
          FROM purchase_miner_project_reward r
          INNER JOIN temp_wallet_addresses t2 ON r.wallet_address = t2.wallet_address
          WHERE r.day_time = p_day_time
            AND r.reward_type = 'NEW'
      );

    -- 3. 回滚矿机奖励数据
    UPDATE purchase_miner_project pmp
    INNER JOIN temp_rollback_amounts ra ON pmp.wallet_address = ra.wallet_address
    SET pmp.reward = CAST(pmp.reward AS DECIMAL(20,8)) - ra.total_reward,
        pmp.reward_price = CAST(pmp.reward_price AS DECIMAL(20,8)) - ra.total_reward_price,
        pmp.update_time = UNIX_TIMESTAMP() * 1000
    WHERE pmp.status = 'SUCCESS';

    -- 4. 删除奖励记录
    DELETE r FROM purchase_miner_project_reward r
    INNER JOIN temp_wallet_addresses t ON r.wallet_address = t.wallet_address
    WHERE r.day_time = p_day_time
      AND r.reward_type = 'NEW';

    -- 5. 删除奖励日志记录
    DELETE rl FROM reward_log rl
    INNER JOIN temp_wallet_addresses t ON rl.wallet_address = t.wallet_address
    WHERE rl.day_time = p_day_time
      AND rl.reward_type = 'NEW';

    -- 清理临时表
    DROP TEMPORARY TABLE temp_wallet_addresses;
    DROP TEMPORARY TABLE temp_rollback_amounts;

    COMMIT;
    
    SELECT '回滚完成' AS result;
END$$

DELIMITER ;

-- 使用示例：
-- CALL RollbackNewPowerRewards('0x1234...,0x5678...,0x9abc...', '2024-01-15');

-- 删除存储过程（执行完成后）
-- DROP PROCEDURE IF EXISTS RollbackNewPowerRewards;
