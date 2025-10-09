package com.example.eco.core.task;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.dto.EtherScanTokenTransferDTO;
import com.example.eco.bean.dto.EtherScanTokenTransferResponseDTO;
import com.example.eco.model.entity.SystemConfig;
import com.example.eco.model.entity.TokenTransferLog;
import com.example.eco.model.mapper.SystemConfigMapper;
import com.example.eco.model.mapper.TokenTransferLogMapper;
import com.example.eco.core.service.TokenTransferService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 代币转账同步任务
 */
@Slf4j
@Component
@Transactional
public class TokenTransferScheduled {

    @Resource
    private SystemConfigMapper systemConfigMapper;

    @Resource
    private TokenTransferLogMapper tokenTransferLogMapper;

    @Resource
    private TokenTransferService tokenTransferService;

    @Value("${ether.scan.transaction.url}")
    private String url;

    /**
     * 每10分钟同步一次代币转账记录
     */
    @Scheduled(fixedRate = 180000) // 10分钟
    @SneakyThrows
    public void syncTokenTransfers() {
        log.info("开始同步代币转账记录");

        // 获取API密钥
        SystemConfig apiKeyConfig = getSystemConfig("API_KEY");
        if (apiKeyConfig == null) {
            log.error("未找到API_KEY配置");
            return;
        }

        // 同步ESG代币转账
        syncTokenTransfersForType("ESG", apiKeyConfig.getValue());

        // 同步ECO代币转账
        syncTokenTransfersForType("ECO", apiKeyConfig.getValue());

        // 检查充值记录
        checkDepositRecords();

        log.info("代币转账记录同步完成");
    }

    /**
     * 同步指定类型的代币转账记录
     */
    @SneakyThrows
    private void syncTokenTransfersForType(String tokenType, String apiKey) {
        log.info("开始同步{}代币转账记录", tokenType);

        // 获取管理合约地址
        String managementContractAddress = getManagementContractAddress(tokenType);
        if (managementContractAddress == null) {
            log.error("未找到{}代币的管理合约地址配置", tokenType);
            return;
        }

        // 获取最后同步的区块号
        Long lastSyncedBlock = getLastSyncedBlock(managementContractAddress, tokenType);

        while (true) {
            log.info("同步{}代币转账记录，当前区块: {}", tokenType, lastSyncedBlock);

            // 构建请求参数
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("address", managementContractAddress);
            paramMap.put("apikey", apiKey);
            paramMap.put("startblock", lastSyncedBlock);
            paramMap.put("module", "account");
            paramMap.put("action", "tokentx");
            paramMap.put("chainid", 1);
            paramMap.put("sort", "asc");

            // 请求Etherscan API
            EtherScanTokenTransferResponseDTO responseDTO;
            try {
                String response = HttpUtil.get(url, paramMap);
                responseDTO = JSONUtil.toBean(response, EtherScanTokenTransferResponseDTO.class);
            } catch (Exception e) {
                log.error("请求Etherscan API失败", e);
                break;
            }

            // 检查响应状态
            if ("0".equals(responseDTO.getStatus())) {
                if ("No records found".equals(responseDTO.getMessage())) {
                    log.info("{}代币在区块{}之后没有更多转账记录", tokenType, lastSyncedBlock);
                    break;
                } else {
                    log.error("Etherscan API返回错误: {}", responseDTO.getMessage());
                    break;
                }
            }

            List<EtherScanTokenTransferDTO> transfers = responseDTO.getResult();
            if (CollectionUtils.isEmpty(transfers)) {
                log.info("{}代币没有更多转账记录需要同步", tokenType);
                break;
            }
            
            // 调试日志：查看API返回的数据
            log.info("Etherscan API返回数据: 状态={}, 记录数={}", responseDTO.getStatus(), transfers.size());
            if (!transfers.isEmpty()) {
                EtherScanTokenTransferDTO firstTransfer = transfers.get(0);
                log.info("第一条转账记录: hash={}, isError={}, from={}, to={}, value={}", 
                        firstTransfer.getHash(), firstTransfer.getIsError(), 
                        firstTransfer.getFrom(), firstTransfer.getTo(), firstTransfer.getValue());
            }

            // 保存转账记录
            int savedCount = saveTokenTransfers(transfers, tokenType);
            log.info("成功同步{}代币{}条转账记录", tokenType, savedCount);

            // 更新区块高度
            lastSyncedBlock = Long.valueOf(transfers.get(transfers.size() - 1).getBlockNumber());

            // API 频率限制
            TimeUnit.SECONDS.sleep(1);

            // 如果返回记录数小于最大值，说明已经同步完成
            if (transfers.size() < 10000) {
                break;
            }
        }
    }

    /**
     * 获取系统配置
     */
    private SystemConfig getSystemConfig(String name) {
        return systemConfigMapper.selectOne(new QueryWrapper<SystemConfig>().eq("`name`", name));
    }

    /**
     * 获取管理合约地址
     */
    private String getManagementContractAddress(String tokenType) {
        String configName = "ESG".equals(tokenType) ? "ESG_ADDRESS" : "ECO_ADDRESS";
        SystemConfig config = getSystemConfig(configName);
        return config != null ? config.getValue() : null;
    }

    /**
     * 获取最后同步的区块号
     */
    private Long getLastSyncedBlock(String managementContractAddress, String tokenType) {
        LambdaQueryWrapper<TokenTransferLog> queryWrapper = new LambdaQueryWrapper<TokenTransferLog>()
                .eq(TokenTransferLog::getToAddress, managementContractAddress)
                .eq(TokenTransferLog::getTokenType, tokenType)
                .orderByDesc(TokenTransferLog::getBlockNumber)
                .last("limit 1");

        TokenTransferLog lastTransfer = tokenTransferLogMapper.selectOne(queryWrapper);
        
        Long lastBlock = Optional.ofNullable(lastTransfer)
                .map(TokenTransferLog::getBlockNumber)
                .orElse(0L);
        
        log.info("获取{}代币最后同步区块: 管理合约地址={}, 最后区块={}", 
                tokenType, managementContractAddress, lastBlock);
        
        return lastBlock;
    }

    /**
     * 保存代币转账记录
     */
    @SneakyThrows
    private int saveTokenTransfers(List<EtherScanTokenTransferDTO> transfers, String tokenType) {
        if (CollectionUtils.isEmpty(transfers)) {
            return 0;
        }

        // 批量查询已存在的转账记录
        List<String> hashList = transfers.stream().map(EtherScanTokenTransferDTO::getHash).collect(Collectors.toList());

        Map<String, TokenTransferLog> existHashMap = tokenTransferLogMapper.selectList(
                new QueryWrapper<TokenTransferLog>()
                        .in("tx_hash", hashList)
        ).stream().collect(Collectors.toMap(TokenTransferLog::getHash, Function.identity()));

        int savedCount = 0;
        for (EtherScanTokenTransferDTO transferDTO : transfers) {
            if (existHashMap.containsKey(transferDTO.getHash())) {
                continue;
            }

            // 创建转账记录
            TokenTransferLog transferLog = new TokenTransferLog();
            transferLog.setHash(transferDTO.getHash());
            transferLog.setTokenAddress(transferDTO.getContractAddress());
            transferLog.setTokenType(tokenType);
            transferLog.setFromAddress(transferDTO.getFrom());
            transferLog.setToAddress(transferDTO.getTo());
            transferLog.setTransferValue(formatTokenAmount(transferDTO.getValue(), transferDTO.getTokenDecimal()));
            transferLog.setBlockNumber(Long.valueOf(transferDTO.getBlockNumber()));
            transferLog.setTransactionIndex(Integer.valueOf(transferDTO.getTransactionIndex()));
            transferLog.setGasUsed(transferDTO.getGasUsed());
            // 判断交易状态：tokentx接口通常不返回isError字段，默认为成功
            // 如果有isError字段且为"1"才表示失败
            String isError = transferDTO.getIsError();
            String status = "1".equals(isError) ? "FAILED" : "SUCCESS";
            transferLog.setStatus(status);
            
            // 调试日志
            log.debug("处理转账记录: hash={}, isError={}, status={}, from={}, to={}, value={}", 
                    transferDTO.getHash(), isError, status, transferDTO.getFrom(), 
                    transferDTO.getTo(), transferDTO.getValue());
            transferLog.setChecked(false);
            transferLog.setCreateTime(System.currentTimeMillis());
            transferLog.setUpdateTime(System.currentTimeMillis());

            tokenTransferLogMapper.insert(transferLog);
            savedCount++;
        }

        return savedCount;
    }

    /**
     * 格式化代币金额
     */
    private String formatTokenAmount(String rawAmount, String decimals) {
        try {
            BigInteger rawValue = new BigInteger(rawAmount);
            int decimalPlaces = Integer.parseInt(decimals);
            
            if (rawValue.equals(BigInteger.ZERO)) {
                return "0";
            }
            
            // 将原始金额除以10^decimals得到实际金额
            BigDecimal divisor = BigDecimal.TEN.pow(decimalPlaces);
            BigDecimal actualAmount = new BigDecimal(rawValue).divide(divisor, decimalPlaces, BigDecimal.ROUND_HALF_UP);
            
            // 去掉尾随的0
            return actualAmount.stripTrailingZeros().toPlainString();
        } catch (Exception e) {
            log.warn("格式化代币金额失败: rawAmount={}, decimals={}", rawAmount, decimals, e);
            return rawAmount;
        }
    }

    /**
     * 检查充值记录
     */
    @SneakyThrows
    private void checkDepositRecords() {
        try {
            log.info("开始检查充值记录");
            
            // 检查ESG代币充值记录
            SingleResponse<String> esgResponse = tokenTransferService.checkDepositRecords("ESG");
            if (esgResponse.isSuccess()) {
                log.info("ESG充值记录检查完成: {}", esgResponse.getData());
            } else {
                log.error("ESG充值记录检查失败: {}", esgResponse.getErrMessage());
            }
            
            // 检查ECO代币充值记录
            SingleResponse<String> ecoResponse = tokenTransferService.checkDepositRecords("ECO");
            if (ecoResponse.isSuccess()) {
                log.info("ECO充值记录检查完成: {}", ecoResponse.getData());
            } else {
                log.error("ECO充值记录检查失败: {}", ecoResponse.getErrMessage());
            }
            
        } catch (Exception e) {
            log.error("检查充值记录异常", e);
        }
    }
}