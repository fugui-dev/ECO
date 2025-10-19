# ESG购买矿机功能实现总结

## 功能概述

实现了ESG购买矿机的新需求，包括：
1. 每天只投放50台ESG矿机（可配置）
2. 定时开放购买（可配置时间）
3. 每个地址每天只能买一台ESG矿机
4. 使用Redis分布式锁防止并发问题
5. 其他购买方式不受限制

## 实现文件

### 1. 配置枚举
- `SystemConfigEnum.java` - 添加了ESG相关配置项
  - `ESG_DAILY_LIMIT` - ESG每日投放数量
  - `ESG_PURCHASE_START_TIME` - ESG购买开始时间

### 2. 核心工具类
- `EsgPurchaseLimitUtil.java` - ESG购买限制管理工具类
  - 检查ESG每日投放限额
  - 检查用户每日购买限制
  - 管理Redis分布式锁
  - 提供统计信息查询

### 3. 定时任务
- `EsgPurchaseScheduled.java` - ESG购买定时任务
  - 每天凌晨0点重置ESG购买计数
  - 每天定时开放ESG购买（默认10点）
  - 每小时检查ESG购买状态

### 4. 业务逻辑修改
- `PurchaseMinerProjectServiceImpl.java` - 修改购买矿机逻辑
  - 集成ESG购买限制检查
  - 在ESG购买成功后更新计数和标记

### 5. 管理接口
- `AdminEsgPurchaseController.java` - 管理员接口
  - 设置/获取ESG每日限制
  - 查看购买统计信息
  - 重置购买计数
  - 手动开放购买

### 6. 用户接口
- `EsgPurchaseController.java` - 用户接口
  - 检查ESG购买可用性
  - 检查用户购买状态
  - 获取购买统计信息

## 功能特性

### 1. 并发安全
- 使用Redis分布式锁确保并发安全
- 原子操作防止超卖问题
- 锁超时机制防止死锁

### 2. 配置灵活
- 每日投放数量可配置
- 购买开放时间可配置
- 支持动态调整限制

### 3. 统计监控
- 实时统计每日购买数量
- 提供剩余数量查询
- 支持历史数据查看

### 4. 用户限制
- 每个地址每天只能购买一台ESG矿机
- 按矿机项目ID分别限制
- 自动过期清理历史数据

## 使用方法

### 1. 配置ESG购买限制
```bash
# 设置每日投放数量为50台
POST /admin/esg-purchase/set-daily-limit?dailyLimit=50

# 获取当前限制
GET /admin/esg-purchase/get-daily-limit
```

### 2. 查看购买统计
```bash
# 查看矿机项目的购买统计
GET /admin/esg-purchase/stats?minerProjectId=1

# 用户查看购买状态
GET /user/esg-purchase/stats?minerProjectId=1
```

### 3. 检查购买资格
```bash
# 检查ESG购买是否可用
GET /user/esg-purchase/check-available?minerProjectId=1

# 检查用户是否已购买
GET /user/esg-purchase/check-user-purchase?walletAddress=0x123&minerProjectId=1
```

## 定时任务配置

### 1. 重置任务
- 执行时间：每天凌晨0点
- 功能：重置ESG购买计数，设置当日限制

### 2. 开放任务
- 执行时间：每天上午10点（可配置）
- 功能：确保ESG购买开放，设置限制

### 3. 状态检查
- 执行时间：每小时
- 功能：检查ESG购买状态，记录日志

## Redis Key设计

### 1. 每日计数
- 格式：`esg:daily:count:yyyy-MM-dd:minerProjectId`
- 类型：AtomicLong
- 过期时间：2天

### 2. 用户购买记录
- 格式：`esg:user:daily:yyyy-MM-dd:walletAddress:minerProjectId`
- 类型：String
- 过期时间：2天

### 3. 分布式锁
- 格式：`esg:purchase:lock:minerProjectId`
- 类型：RLock
- 超时时间：10秒

### 4. 每日限制
- 格式：`esg:daily:limit`
- 类型：AtomicLong
- 默认值：50

## 错误处理

### 1. 限额检查
- 每日投放达上限：返回"今日ESG矿机投放已达上限，请明日再试"
- 用户已购买：返回"您今日已购买过该ESG矿机，每个地址每天只能购买一台"

### 2. 并发处理
- 获取锁失败：返回"系统繁忙，请稍后重试"
- 锁超时：自动释放锁，记录警告日志

### 3. 异常处理
- 配置错误：使用默认值并记录警告
- Redis异常：记录错误日志，返回失败响应

## 监控建议

### 1. 关键指标
- 每日ESG购买数量
- 购买成功率
- 锁获取失败率
- Redis连接状态

### 2. 告警设置
- 购买数量接近限制时告警
- 锁获取失败率过高时告警
- Redis连接异常时告警

### 3. 日志监控
- 关注WARN和ERROR级别日志
- 监控购买流程关键节点
- 记录异常情况便于排查

## 扩展建议

### 1. 功能扩展
- 支持不同矿机项目不同限制
- 支持VIP用户特殊限制
- 支持购买时间段限制

### 2. 性能优化
- 使用Redis Pipeline批量操作
- 优化锁粒度减少竞争
- 添加本地缓存减少Redis访问

### 3. 监控完善
- 添加Prometheus指标
- 集成Grafana仪表板
- 实现自动告警机制
