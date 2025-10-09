-- 创建代币转账记录表
-- 请先执行这个SQL脚本创建表

DROP TABLE IF EXISTS `token_transfer_log`;

CREATE TABLE `token_transfer_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tx_hash` varchar(66) NOT NULL COMMENT '交易哈希',
  `token_address` varchar(42) NOT NULL COMMENT '代币合约地址',
  `token_type` varchar(10) NOT NULL COMMENT '代币类型(ESG/ECO)',
  `from_address` varchar(42) NOT NULL COMMENT '发送方地址',
  `to_address` varchar(42) NOT NULL COMMENT '接收方地址',
  `transfer_value` varchar(50) NOT NULL COMMENT '转账金额',
  `block_number` bigint(20) NOT NULL COMMENT '区块号',
  `transaction_index` int(11) NOT NULL COMMENT '交易索引',
  `gas_used` varchar(20) DEFAULT NULL COMMENT 'Gas使用量',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '交易状态(SUCCESS/FAILED/PENDING)',
  `checked` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已检查',
  `create_time` bigint(20) NOT NULL COMMENT '创建时间',
  `update_time` bigint(20) NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tx_hash` (`tx_hash`),
  KEY `idx_token_type` (`token_type`),
  KEY `idx_from_address` (`from_address`),
  KEY `idx_to_address` (`to_address`),
  KEY `idx_block_number` (`block_number`),
  KEY `idx_status` (`status`),
  KEY `idx_checked` (`checked`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代币转账记录表';

-- 插入系统配置（如果不存在）
INSERT IGNORE INTO system_config (`name`, `value`, `description`) VALUES 
('API_KEY', 'HGWSZWJUE5PV3A93S2CU9F4F5EUN9CGK63', 'Etherscan API密钥'),
('ESG_ADDRESS', '0x4fC335d41CCAfBa07d2499f994965E4d142440E8', 'ESG代币管理合约地址'),
('ECO_ADDRESS', '0xe729fE3c368B36F7dEa0Ed545426ff7c6bFA4c0D', 'ECO代币管理合约地址');
