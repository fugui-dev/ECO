-- 完整修复版回滚脚本
-- 解决所有临时表冲突问题

-- ==========================================
-- 配置参数
-- ==========================================

SET @target_date = '2025-10-09';  -- 目标日期

-- ==========================================
-- 第一步：清理可能存在的临时表
-- ==========================================

DROP TEMPORARY TABLE IF EXISTS target_wallets_rollback;
DROP TEMPORARY TABLE IF EXISTS rollback_calculations_temp;
DROP TEMPORARY TABLE IF EXISTS temp_orders_to_delete;

-- ==========================================
-- 第二步：创建临时表存储目标地址
-- ==========================================

CREATE TEMPORARY TABLE target_wallets_rollback (
    wallet_address VARCHAR(255) PRIMARY KEY
);

-- 插入目标地址
INSERT INTO target_wallets_rollback (wallet_address) VALUES 
('0x8ee830de0cd81de591de840518e0f87902d01468'),
('0x61809676ef8f5e4bad14514aee2dd0c9fd1f0c94'),
('0x925e88137dbd08cd0e9ac67c829ec505a1a597c6'),
('0x95066f226ce7e00c28acff0a34881b5dc18a69ce'),
('0x07d98e967ed89402c33291222438d2435a9229f5'),
('0x9a9e174f1d95d6fac5e1a9661a5447353a53ec7b'),
('0x4e4c61dcf2629a2186e1cb63ae8d96c93df695f0'),
('0x0186f88fc78327927d31f126f0c1b09e3092c2c4'),
('0x585bd44e61cc21ad75f37d42710f8fcda213ff29'),
('0xad6f3a147f779f513d99d81bfdfb5ccae56e419a'),
('0x6719df52dd296788faa617fa01204271c1acc0e7'),
('0xb081a300bd330ea477599c2abb1ec32f0adf0970'),
('0xdde90c87802ba7a23d097834b513f5aa3df75a90'),
('0x1770073636f69f68ee97b2b9cf9dc33d2704cfe1'),
('0x7b6672ef0580dd7703897d3bbdc126967afc9a11'),
('0x39ea13fd18a122a5c877c003e8c752669e9666b8'),
('0xf23edf2461d95e292e30986d57ec1f695caff668'),
('0x6eb3e4d77b6efb22870b2c76389458e49018f111'),
('0x67e4427d4299191dadcd03d9869041da013c36da'),
('0xf602375e83413eb75a3c953d7c139df49285d3a7'),
('0xe4b38409667741969d1b2515a33845d17a6b3abd'),
('0x9c7d2271e4340737b872db9ff392e63233926747'),
('0x9f207b600aad33cc6d0bd0bf55677f5dc07f5c91'),
('0xa2347e136592164fb9c1ada2a08626f4e3c8fd44'),
('0x2afaf02ffb08a7cce71b3cef8ee3a0de053b7f7b'),
('0x91da5ffe0196e5413fbec6c91a961fefae3471c1'),
('0x8eb1878120bcac783e37131d4086542a79bc3c8a'),
('0x743c6904b410d147cde1f2319cc18ae718abeda7'),
('0x54a037370c705950797f2b0fe3bbbae8567f0f78'),
('0x5e0fe43a4911b45eba5e9261f05110fd09fcc147'),
('0x53c1313994a9013f4edf6a7dad73de8b487d0f46'),
('0x839f0e9c8634daeb0789a34581ffdb6e33709a5e'),
('0x0f151e325a254046f79fd11a17668df0e870bbf1'),
('0x3327c45c0e31d8e246cedc8a16f65d4859661b47'),
('0x35d6045d253ae9d2b303053f0b2fc88e55b97d0a'),
('0xda4a3562df9541fb11b839690e0931d11cbe8ff5'),
('0x87482d50b648b5146bc8335babbd2061e340e385'),
('0x89ec26f31933bd7daf68d22e20bb590dd268f96f'),
('0xc6a9bb2ee3f6a6a53f7c3c2cae8617366ae976e3'),
('0x1cd372a298add771f6f1f8a4402bd8d5313429e2'),
('0xdeee9a08ab4f258aab7b47bcfb8f3de13af229a8'),
('0xb4f24af3503da5b3b0dd1c2fdd7f142906e58ccd'),
('0xdfe9b05b27ad32bfefecbbf6f5de5944e85c94af'),
('0xe3709f60c88aad9ebc4bc0f4191649d8d6025f90'),
('0x7e2f27c8aa5dd865f00797937d30db643e1299ef'),
('0xd5775a21edcfc7d27096f80d6ece2f08decf4bcc'),
('0xf1965c9ba645f6d7065e9d1ea24c12d7056e8285'),
('0x5de216ac8da5df862ad7ac992aede67b539ab808'),
('0x1425b37aaa3fb50855180a5fdbc7970d7c4cac26'),
('0xd85e522161430cb05e91776d2808d7cb09d1a9a7'),
('0x87a87d81abf3f54ccc05f33379b609bed0119f2e'),
('0x3f3a9133fc03737a110bcf4bc08d174b94d84c8d'),
('0x0f7b25113d56c82a11bb9822fe142c6b75e86cdc'),
('0xc4e6ab4ee0af504372be3ee221480e4c0409e9d5'),
('0x7335811ef76afbd1ad54750b29c569e20233267c'),
('0x618eddc2c1c515cf88154a113e971aaedb10474b'),
('0x0571461a4a77cb8ef7a6e45794d521542521a8b3'),
('0x1d0a6cbb5ca738a339c234b1ea9f48ccc4265a94'),
('0xf2f668b454689d4a5b6d0153f4e5055121360855'),
('0x2d7ba704d19a5ac57830d230dc19d6bd9d6712b5'),
('0x08250d8e3136de691960f672688df60bcdc635f2'),
('0x130a9053c33a16fd5a5a6143db005e5f42613e7b'),
('0x8ee85e342b618f4bdeff59a892568cfd92d7b072'),
('0xab9ec42a38a2078ddba81e4d86822992cea36dd4'),
('0xcfbee58d37593b1160597c342a248565836771cb'),
('0x4b1055c6e48416583769f35e455217924bfd19c2'),
('0x456239ee4c6f03a6fc872984d34b3baa54d26255'),
('0x8fea624d69ede21bd147c945f5f47d7eac2e0004'),
('0xa992675bfd2396b117946efef3a763823f91a67f'),
('0xfafa4d8bda2ee34af632eff6c6f2579792104bdb'),
('0xc4c515a1ea682c85ec2ee0349ac9dab589a5d6e2'),
('0x76b4a383051d595d942c8e62f31245fab34e21e7'),
('0x7f3f9ddcfcd3c73d5fe170cfd718ea2347767ef1'),
('0x42c17495cfab959a44349098b6c8ffe035edfdbc'),
('0xb6dacb15808907df7d936eb6ffc30f5e993d65ea'),
('0x0681bb78ea968ee7338dfd9c709ab9abcdffbb0b'),
('0x649dc3cf1f528c55935fc5687ad8a9bc6ab3fcdd'),
('0x384a0abe93ad1c70746418f7ff63ad772873e282'),
('0x0a61ed051b2b6e6c20aaa0add74d6e5b4b7dcecd'),
('0xa8de92e59b4195c1fa3fd849de7b42e2538c52ac'),
('0x4c299b6a5173caa38a174a2fd4a6e5b45463e04c'),
('0x641240a3e22630e97309f9f57179b5c323171888'),
('0xc8669f4403255d747cdbd79dadde84230a335046'),
('0xddb6f37456a908af02bea9f8d07e68db53a9e4f6'),
('0xab414e5c81b6db81ca1a45ef867de52475cb3e2b'),
('0x48f29fa24096f178796c051a3a41f800e8ec6c52'),
('0x50ac5bb22b9edeae02a6e6640e4130ea0059c906'),
('0x7939a78ccb9a36f83751a3ed0a131e8bc848d129'),
('0xd40616b3167adcf86f449c200cdcf24a0efe7093'),
('0xc9f2cace3770f5f24fae06de59cd8f276a52083f'),
('0xf047524c7620e34918ce9358dfa5384614c7213f'),
('0x86abdc3fa0a80b4b5ff76fe81e6edf6640834f7a'),
('0xbc988d758186637b51339fbb479ea4088447f460'),
('0x1499dee1c148767893a15f5050857f39ee98f2cc'),
('0x4723ebab625027b6a2eb8bc34ccf4e307628046c'),
('0xfe4d5ef03ce06163d5cbbef02e7858302e0f3d4c'),
('0x16cd032c597057fc1fcefc66b70f4e0f343287cc'),
('0x770d56cce6c38235e40ce3025692d641130a458e'),
('0x53c088e02285db4bade93434cdece5b583a1d28e'),
('0xf54751673d0ba58b7561293a5fe5a23485dc0bbe'),
('0x446fbad0a354f7c338b9e5814a0f72c033c95b05'),
('0x8c87c620c90de4ccdcad9ee9e47891a27bb13e50'),
('0x39dbafc8536f159655a1d935fb7b1fa4b2cfbb0b'),
('0xfe033431dcefd92b16d9e1c38de837a528921ad0'),
('0xd30dfca90f145192aff4d23879061d27b2f681dc'),
('0xd564622b15f15bea2fbe4e22b7c8571acd389166'),
('0x4299c41c23c08f4c687dbf88142d0ae15b9b29d0'),
('0x67e46bf37eb3b8a46ae962ddcf1fe4f446afaa34'),
('0xd45161b090eadba259c5433fa9c00013afa0a8e8'),
('0xa9225839d79fc6198b25663847522f0716f8d4f6'),
('0xf7f51d8ef1d2047070ca590192b973216aed18eb'),
('0x6397bbc3e4c14ed7c844fd7db9cd1627d3361deb'),
('0x53b7864242ec19f3a93c7114c8516bd1e7dead56'),
('0xb31c0e7d7ff8434894d96efeb1dc1c25a758efbb'),
('0x3686d8fddf22f78e31d1ea4577ea29dd1e44f191'),
('0xa44e6b7ca9203938e9dbd6361b29ca98fc4e0ddc'),
('0x3e9d7c78393d587fbcf519d4d287b04d130ea02b'),
('0x22c0a1d4f410113e1cbf471125ea3ef6d2b76ac5'),
('0x8b7299f5095d59bbe054ce8690aaef37b2037b99'),
('0x43749202cddb8029502079a8a283e9d0013e011f'),
('0x619db0c63801f8308ea21d0096056049c224ba64'),
('0x03363b3115daa1655e1cc93fdd469a1db0aef6ec'),
('0x595fa49d4428bb3ab723688fa6cc5151cb373061'),
('0x9977621b115d29698f0af47273a182a5491dee1b'),
('0x15463064d361b1427f80b357e04bc49cef810877'),
('0x342bf0f8bc5f5055e70a2f82ee20d739a538d26b'),
('0x46589393214a9dc85d0dfe1e71a72763e4507482'),
('0x030849f5f9fc7e913e45aa19065928ce70c8b1ba'),
('0x2809ed641d86c8370dcdf2a36480baf2b624e289'),
('0x0261ccdd38648dd9fbfe2b04d145498f076abc8d'),
('0x6ce0f6131fa80079d009b1b6868d48b025a219e7'),
('0x24b29816c9bb382366ec32f32e11db6cb9deb7eb'),
('0x5d793a092e200f9628b9e0de51daba46b8d6f2d2'),
('0xc050017b00e29fe9001acaeb5d11c586c7655916'),
('0xb0a60e24d663320182990724689e5677a6051ed4'),
('0x0531daa17379fdc3f3b9b011b9bd0ebe838bea74'),
('0x9a20f194ede8669dd4638087ad6e93e6924432d2'),
('0xe2f38aad0e36fe07e252478cf329c85783c29ea7'),
('0x296c7fe8aa4109ba506392c1c43d9aaab741398f'),
('0x00135361cc98778d87c214db58d88169217b8145');

-- ==========================================
-- 第三步：备份数据
-- ==========================================

-- 备份账户数据
CREATE TABLE IF NOT EXISTS backup_account_rollback_20251009 AS 
SELECT * FROM account 
WHERE wallet_address IN (SELECT wallet_address FROM target_wallets_rollback)
  AND type = 'ECO';

-- 备份奖励记录
CREATE TABLE IF NOT EXISTS backup_reward_rollback_20251009 AS 
SELECT * FROM purchase_miner_project_reward 
WHERE wallet_address IN (SELECT wallet_address FROM target_wallets_rollback)
  AND day_time = @target_date
  AND reward_type = 'NEW';

-- ==========================================
-- 第四步：计算回滚金额
-- ==========================================

CREATE TEMPORARY TABLE rollback_calculations_temp AS
SELECT 
    r.wallet_address,
    SUM(CAST(r.reward AS DECIMAL(20,8))) AS total_reward,
    SUM(CAST(r.reward_price AS DECIMAL(20,8))) AS total_reward_price,
    COUNT(*) AS reward_count
FROM purchase_miner_project_reward r
INNER JOIN target_wallets_rollback t ON r.wallet_address = t.wallet_address
WHERE r.day_time = @target_date
  AND r.reward_type = 'NEW'
GROUP BY r.wallet_address;

-- 显示回滚金额统计
SELECT 
    '回滚统计' AS description,
    COUNT(*) AS user_count,
    SUM(total_reward) AS total_reward_amount,
    SUM(total_reward_price) AS total_reward_price_amount,
    SUM(reward_count) AS total_reward_records
FROM rollback_calculations_temp;

-- ==========================================
-- 第五步：执行回滚操作
-- ==========================================

-- 5.1 回滚账户余额
UPDATE account a
INNER JOIN rollback_calculations_temp rc ON a.wallet_address = rc.wallet_address
SET a.number = CAST(a.number AS DECIMAL(20,8)) - rc.total_reward,
    a.dynamic_reward = CAST(a.dynamic_reward AS DECIMAL(20,8)) - rc.total_reward,
    a.dynamic_reward_price = CAST(a.dynamic_reward_price AS DECIMAL(20,8)) - rc.total_reward_price,
    a.update_time = UNIX_TIMESTAMP() * 1000
WHERE a.type = 'ECO';

-- 5.2 删除账户交易记录（修复版）
-- 先获取需要删除的订单号
CREATE TEMPORARY TABLE temp_orders_to_delete AS
SELECT DISTINCT `order` 
FROM purchase_miner_project_reward r
INNER JOIN target_wallets_rollback t2 ON r.wallet_address = t2.wallet_address
WHERE r.day_time = @target_date
  AND r.reward_type = 'NEW';

-- 删除账户交易记录
DELETE at FROM account_transaction at
INNER JOIN target_wallets_rollback t ON at.wallet_address = t.wallet_address
WHERE at.transaction_type IN ('DYNAMIC_REWARD', 'ADD_NUMBER')
  AND at.`order` IN (SELECT `order` FROM temp_orders_to_delete);

-- 5.3 回滚矿机奖励数据
UPDATE purchase_miner_project pmp
INNER JOIN rollback_calculations_temp rc ON pmp.wallet_address = rc.wallet_address
SET pmp.reward = CAST(pmp.reward AS DECIMAL(20,8)) - rc.total_reward,
    pmp.reward_price = CAST(pmp.reward_price AS DECIMAL(20,8)) - rc.total_reward_price,
    pmp.update_time = UNIX_TIMESTAMP() * 1000
WHERE pmp.status = 'SUCCESS';

-- 5.4 删除奖励记录
DELETE r FROM purchase_miner_project_reward r
INNER JOIN target_wallets_rollback t ON r.wallet_address = t.wallet_address
WHERE r.day_time = @target_date
  AND r.reward_type = 'NEW';

-- 5.5 删除奖励日志记录
DELETE rl FROM reward_log rl
INNER JOIN target_wallets_rollback t ON rl.wallet_address = t.wallet_address
WHERE rl.day_time = @target_date
  AND rl.reward_type = 'NEW';

-- ==========================================
-- 第六步：验证回滚结果
-- ==========================================

-- 验证账户余额
SELECT 
    '回滚后账户余额' AS description,
    COUNT(*) AS account_count,
    SUM(CAST(number AS DECIMAL(20,8))) AS total_balance,
    SUM(CAST(dynamic_reward AS DECIMAL(20,8))) AS total_dynamic_reward,
    SUM(CAST(dynamic_reward_price AS DECIMAL(20,8))) AS total_dynamic_reward_price
FROM account 
WHERE wallet_address IN (SELECT wallet_address FROM target_wallets_rollback)
  AND type = 'ECO';

-- 验证奖励记录是否已删除
SELECT 
    '剩余奖励记录数' AS description,
    COUNT(*) AS count
FROM purchase_miner_project_reward 
WHERE wallet_address IN (SELECT wallet_address FROM target_wallets_rollback)
  AND day_time = @target_date
  AND reward_type = 'NEW';

-- 验证交易记录是否已删除
SELECT 
    '剩余交易记录数' AS description,
    COUNT(*) AS count
FROM account_transaction 
WHERE wallet_address IN (SELECT wallet_address FROM target_wallets_rollback)
  AND transaction_type IN ('DYNAMIC_REWARD', 'ADD_NUMBER');

-- ==========================================
-- 第七步：清理临时表
-- ==========================================

DROP TEMPORARY TABLE target_wallets_rollback;
DROP TEMPORARY TABLE rollback_calculations_temp;
DROP TEMPORARY TABLE temp_orders_to_delete;

-- ==========================================
-- 回滚脚本（如果需要恢复）
-- ==========================================

/*
-- 如果需要恢复到回滚前的状态：

-- 恢复账户数据
UPDATE account a
INNER JOIN backup_account_rollback_20251009 b ON a.wallet_address = b.wallet_address
SET a.number = b.number,
    a.dynamic_reward = b.dynamic_reward,
    a.dynamic_reward_price = b.dynamic_reward_price,
    a.update_time = b.update_time
WHERE a.type = b.type;

-- 恢复奖励记录
INSERT INTO purchase_miner_project_reward 
SELECT * FROM backup_reward_rollback_20251009;
*/
