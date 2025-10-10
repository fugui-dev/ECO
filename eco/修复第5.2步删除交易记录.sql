-- 修复第5.2步：删除账户交易记录
-- 解决 "Can't reopen table: 't'" 错误

-- 方法1：先获取需要删除的订单号，然后删除
CREATE TEMPORARY TABLE temp_orders_to_delete AS
SELECT DISTINCT `order` 
FROM purchase_miner_project_reward r
INNER JOIN target_wallets_rollback t2 ON r.wallet_address = t2.wallet_address
WHERE r.day_time = '2025-10-09'
  AND r.reward_type = 'NEW';

-- 删除账户交易记录
DELETE at FROM account_transaction at
INNER JOIN target_wallets_rollback t ON at.wallet_address = t.wallet_address
WHERE at.transaction_type IN ('DYNAMIC_REWARD', 'ADD_NUMBER')
  AND at.`order` IN (SELECT `order` FROM temp_orders_to_delete);

-- 清理临时表
DROP TEMPORARY TABLE temp_orders_to_delete;
