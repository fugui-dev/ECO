-- 创建算力奖励档位记录表
CREATE TABLE IF NOT EXISTS `power_reward_level` (
  `id` int NOT NULL AUTO_INCREMENT,
  `day_time` varchar(20) NOT NULL COMMENT '日期',
  `computing_power` varchar(50) NOT NULL COMMENT '算力',
  `level` int NOT NULL COMMENT '档位级别',
  `reward_amount` varchar(50) NOT NULL COMMENT '奖励数量',
  `base_power` varchar(50) NOT NULL COMMENT '每档算力差距',
  `level_add_size` varchar(50) NOT NULL COMMENT '每档奖励数量',
  `create_time` bigint NOT NULL COMMENT '创建时间',
  `update_time` bigint NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_day_level` (`day_time`, `level`),
  KEY `idx_day_time` (`day_time`),
  KEY `idx_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='算力奖励档位记录表';

