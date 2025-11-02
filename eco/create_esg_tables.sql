-- 创建ESG相关表

-- 1. ESG账户表
CREATE TABLE IF NOT EXISTS `esg_account` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `wallet_address` varchar(42) NOT NULL COMMENT '钱包地址',
  `number` varchar(50) NOT NULL DEFAULT '0' COMMENT '账号可以积分',
  `charge_number` varchar(50) NOT NULL DEFAULT '0' COMMENT '充值数量',
  `charge_lock_number` varchar(50) NOT NULL DEFAULT '0' COMMENT '充值锁定数量',
  `static_reward` varchar(50) NOT NULL DEFAULT '0' COMMENT '静态收益',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `create_time` bigint NOT NULL COMMENT '创建时间',
  `update_time` bigint NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wallet_address` (`wallet_address`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ESG账户表';

-- 2. ESG账户交易记录表
CREATE TABLE IF NOT EXISTS `esg_account_transaction` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `wallet_address` varchar(42) NOT NULL COMMENT '钱包地址',
  `account_id` int NOT NULL COMMENT '账号ID',
  `transaction_type` varchar(50) NOT NULL COMMENT '交易类型',
  `number` varchar(50) NOT NULL COMMENT '交易数量',
  `before_number` varchar(50) NOT NULL COMMENT '交易前余额',
  `after_number` varchar(50) NOT NULL COMMENT '交易后余额',
  `transaction_time` bigint NOT NULL COMMENT '交易时间',
  `hash` varchar(66) DEFAULT NULL COMMENT '交易哈希',
  `order` varchar(100) DEFAULT NULL COMMENT '订单号',
  `status` varchar(20) NOT NULL COMMENT '状态',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_wallet_address` (`wallet_address`),
  KEY `idx_account_id` (`account_id`),
  KEY `idx_transaction_type` (`transaction_type`),
  KEY `idx_hash` (`hash`),
  KEY `idx_order` (`order`),
  KEY `idx_status` (`status`),
  KEY `idx_transaction_time` (`transaction_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ESG账户交易记录表';

-- 3. ESG充值订单表
CREATE TABLE IF NOT EXISTS `esg_charge_order` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order` varchar(100) NOT NULL COMMENT '订单号',
  `wallet_address` varchar(42) NOT NULL COMMENT '钱包地址',
  `number` varchar(50) NOT NULL COMMENT '数量',
  `status` varchar(20) NOT NULL COMMENT '状态',
  `hash` varchar(66) DEFAULT NULL COMMENT '交易哈希',
  `create_time` bigint NOT NULL COMMENT '创建时间',
  `finish_time` bigint DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order` (`order`),
  KEY `idx_wallet_address` (`wallet_address`),
  KEY `idx_status` (`status`),
  KEY `idx_hash` (`hash`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ESG充值订单表';

-- 4. ESG矿机项目表
CREATE TABLE IF NOT EXISTS `esg_miner_project` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `price` varchar(50) NOT NULL COMMENT '价格',
  `computing_power` varchar(50) NOT NULL COMMENT '矿机算力',
  `rate` varchar(50) NOT NULL COMMENT '矿机挖矿速率',
  `status` int NOT NULL DEFAULT '1' COMMENT '矿机状态 1 开启 0 关闭',
  `create_time` bigint NOT NULL COMMENT '创建时间',
  `update_time` bigint NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ESG矿机项目表';

-- 5. ESG购买矿机项目表
CREATE TABLE IF NOT EXISTS `esg_purchase_miner_project` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order` varchar(100) NOT NULL COMMENT '订单号',
  `wallet_address` varchar(42) NOT NULL COMMENT '钱包地址',
  `miner_project_id` int NOT NULL COMMENT '矿机项目ID',
  `price` varchar(50) NOT NULL COMMENT '价格',
  `computing_power` varchar(50) NOT NULL COMMENT '矿机算力',
  `status` varchar(20) NOT NULL COMMENT '状态',
  `reward` varchar(50) NOT NULL DEFAULT '0' COMMENT '产生奖励数量',
  `yesterday_reward` varchar(50) NOT NULL DEFAULT '0' COMMENT '昨天产生奖励数量',
  `reason` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `create_time` bigint NOT NULL COMMENT '创建时间',
  `update_time` bigint NOT NULL COMMENT '更新时间',
  `finish_time` bigint DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order` (`order`),
  KEY `idx_wallet_address` (`wallet_address`),
  KEY `idx_miner_project_id` (`miner_project_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ESG购买矿机项目表';

-- 6. ESG购买矿机项目奖励表
CREATE TABLE IF NOT EXISTS `esg_purchase_miner_project_reward` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order` varchar(100) NOT NULL COMMENT '订单号',
  `esg_purchase_miner_project_id` int NOT NULL COMMENT '购买矿机项目ID',
  `wallet_address` varchar(42) NOT NULL COMMENT '钱包地址',
  `reward` varchar(50) NOT NULL COMMENT '奖励',
  `computing_power` varchar(50) NOT NULL COMMENT '矿机算力',
  `rate` varchar(50) NOT NULL COMMENT '矿机挖矿速率',
  `day_time` varchar(20) NOT NULL COMMENT '奖励时间',
  `create_time` bigint NOT NULL COMMENT '创建时间',
  `update_time` bigint NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order`),
  KEY `idx_esg_purchase_miner_project_id` (`esg_purchase_miner_project_id`),
  KEY `idx_wallet_address` (`wallet_address`),
  KEY `idx_day_time` (`day_time`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ESG购买矿机项目奖励表';

-- 7. EtherScan账户交易记录表
CREATE TABLE IF NOT EXISTS `ether_scan_account_transaction` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `block_number` bigint DEFAULT NULL COMMENT '区块号',
  `block_hash` varchar(66) DEFAULT NULL COMMENT '区块哈希',
  `time_stamp` varchar(20) DEFAULT NULL COMMENT '时间戳',
  `hash` varchar(66) DEFAULT NULL COMMENT '交易哈希',
  `nonce` varchar(20) DEFAULT NULL COMMENT '随机数',
  `transaction_index` varchar(20) DEFAULT NULL COMMENT '交易索引',
  `from` varchar(42) DEFAULT NULL COMMENT '发送方地址',
  `to` varchar(42) DEFAULT NULL COMMENT '接收方地址',
  `value` varchar(50) DEFAULT NULL COMMENT '交易金额',
  `gas` varchar(20) DEFAULT NULL COMMENT 'Gas限制',
  `gas_price` varchar(20) DEFAULT NULL COMMENT 'Gas价格',
  `input` text COMMENT '输入数据',
  `decoded_input` text COMMENT '解码后的输入数据',
  `method_id` varchar(10) DEFAULT NULL COMMENT '方法ID',
  `function_name` varchar(100) DEFAULT NULL COMMENT '函数名',
  `contract_address` varchar(42) DEFAULT NULL COMMENT '合约地址',
  `is_error` varchar(10) DEFAULT NULL COMMENT '是否错误',
  `cumulative_gas_used` varchar(20) DEFAULT NULL COMMENT '累计Gas使用量',
  `gas_used` varchar(20) DEFAULT NULL COMMENT 'Gas使用量',
  `confirmations` varchar(20) DEFAULT NULL COMMENT '确认数',
  `receipt_status` varchar(20) DEFAULT NULL COMMENT '收据状态',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hash` (`hash`),
  KEY `idx_block_number` (`block_number`),
  KEY `idx_from` (`from`),
  KEY `idx_to` (`to`),
  KEY `idx_contract_address` (`contract_address`),
  KEY `idx_time_stamp` (`time_stamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='EtherScan账户交易记录表';

