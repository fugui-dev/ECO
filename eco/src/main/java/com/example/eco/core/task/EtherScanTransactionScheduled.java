package com.example.eco.core.task;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.eco.bean.dto.BscScanAccountTransactionDTO;
import com.example.eco.bean.dto.BscScanAccountTransactionResponseDTO;
import com.example.eco.model.entity.EtherScanAccountTransaction;
import com.example.eco.model.entity.SystemConfig;
import com.example.eco.model.mapper.EtherScanAccountTransactionMapper;
import com.example.eco.model.mapper.SystemConfigMapper;
import com.example.eco.util.InputDataDecoderUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@Transactional
public class EtherScanTransactionScheduled {


    @Resource
    private SystemConfigMapper systemConfigMapper;

    @Resource
    private EtherScanAccountTransactionMapper etherScanAccountTransactionMapper;

    @Value("${ether.scan.transaction.url}")
    private String url;

    @Scheduled(cron = "0 0/3 * * * ?")
    @SneakyThrows
    public void transactionAccountRecord() {

        log.info("transactionAccountRecord 开始执行");

        QueryWrapper<SystemConfig> apikey = new QueryWrapper<>();
        apikey.eq("`name`", "apikey");

        SystemConfig apikeyConfig = systemConfigMapper.selectOne(apikey);
        if (Objects.isNull(apikeyConfig)) {
            return;
        }


        QueryWrapper<SystemConfig> contractAddress = new QueryWrapper<>();
        contractAddress.eq("`name`", "contractAddress");

        SystemConfig contractAddressConfig = systemConfigMapper.selectOne(contractAddress);
        if (Objects.isNull(contractAddressConfig)) {
            return;
        }
        getBscScanAccountTransactionResponse(apikeyConfig.getValue(), contractAddressConfig.getValue());

        log.info("transactionAccountRecord 结束执行");

    }


    @SneakyThrows
    public void getBscScanAccountTransactionResponse(String apikey, String contractAddress) {

        // 获取最后同步的区块
        Long blockNumber = getLastSyncedBlock(contractAddress);

        while (true) {
            log.info("开始同步区块，当前区块: {}", blockNumber);

            // 构建请求参数
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("address", contractAddress);
            paramMap.put("apikey", apikey);
            paramMap.put("startblock", blockNumber);
            paramMap.put("module", "account");
            paramMap.put("action", "txlist");
            paramMap.put("sort", "asc");

            // 请求BSCscan API
            BscScanAccountTransactionResponseDTO responseDTO;
            try {
                String response = HttpUtil.get(url, paramMap);

                responseDTO = JSONUtil.toBean(response, BscScanAccountTransactionResponseDTO.class);
            } catch (Exception e) {
                log.error("请求BSCscan API失败", e);
                break;
            }

            // 检查响应状态
            if ("0".equals(responseDTO.getStatus())) {
                log.error("BSCscan API返回错误: {}", responseDTO.getMessage());
                break;
            }

            List<BscScanAccountTransactionDTO> transactions = responseDTO.getResult();
            if (CollectionUtils.isEmpty(transactions)) {
                log.info("没有更多交易记录需要同步");
                break;
            }

            // 保存交易记录
            saveBscScanAccountTransaction(responseDTO);
            log.info("成功同步 {} 条交易记录", transactions.size());

            // 更新区块高度
            blockNumber = Long.valueOf(transactions.get(transactions.size() - 1).getBlockNumber());

            // API 频率限制
            TimeUnit.SECONDS.sleep(1);

            // 如果返回记录数小于最大值，说明已经同步完成
            if (transactions.size() < 10000) {
                break;
            }
        }
    }

    /**
     * 获取区号
     *
     * @param contractAddress
     * @return
     */
    private Long getLastSyncedBlock(String contractAddress) {
        LambdaQueryWrapper<EtherScanAccountTransaction> queryWrapper =
                new LambdaQueryWrapper<EtherScanAccountTransaction>().eq(EtherScanAccountTransaction::getTo, contractAddress)
                        .orderByDesc(EtherScanAccountTransaction::getBlockNumber)
                        .last("limit 1");

        EtherScanAccountTransaction lastTransaction = etherScanAccountTransactionMapper.selectOne(queryWrapper);

        return Optional.ofNullable(lastTransaction)
                .map(EtherScanAccountTransaction::getBlockNumber)
                .orElse(0L);
    }

    /**
     * 存储交易记录
     */
    @SneakyThrows
    public void saveBscScanAccountTransaction(BscScanAccountTransactionResponseDTO responseDTO) {
        if (responseDTO == null || CollectionUtils.isEmpty(responseDTO.getResult())) {
            return;
        }

        List<BscScanAccountTransactionDTO> transactionList = responseDTO.getResult();

        // 批量查询已存在的交易记录
        List<String> hashList = transactionList.stream()
                .map(BscScanAccountTransactionDTO::getHash)
                .collect(Collectors.toList());

        Map<String, EtherScanAccountTransaction> existHashMap = etherScanAccountTransactionMapper.selectList(
                new QueryWrapper<EtherScanAccountTransaction>().in("`hash`", hashList)
        ).stream().collect(Collectors.toMap(EtherScanAccountTransaction::getHash, Function.identity()));


        for (BscScanAccountTransactionDTO bscScanAccountTransactionDTO : transactionList) {
            if (existHashMap.containsKey(bscScanAccountTransactionDTO.getHash())) {
                continue;
            }

            // 保存交易记录
            EtherScanAccountTransaction transaction = new EtherScanAccountTransaction();
            BeanUtils.copyProperties(bscScanAccountTransactionDTO, transaction);
            transaction.setBlockNumber(Long.valueOf(bscScanAccountTransactionDTO.getBlockNumber()));
            transaction.setReceiptStatus(bscScanAccountTransactionDTO.getTxreceipt_status());
            InputDataDecoderUtil.BscScanAccountTransaction(transaction);
            etherScanAccountTransactionMapper.insert(transaction);

            if (bscScanAccountTransactionDTO.getIsError().equals("1")) {
                continue;
            }
        }
    }

}
