package com.example.eco.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eco.model.entity.TokenTransferLog;
import com.example.eco.model.mapper.TokenTransferLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * 交易验证工具类
 */
@Slf4j
@Component
public class TransactionVerificationUtil {

    @Resource
    private TokenTransferLogMapper tokenTransferLogMapper;
    
    /**
     * 验证链上交易（包含接收方地址验证）
     * @param transactionHash 交易哈希
     * @param expectedAmount 期望的交易数量（代币单位，如：10000）
     * @param tokenType 代币类型 (ECO/ESG)
     * @return 验证结果
     */
    public Boolean verifyTransaction(String transactionHash, String expectedAmount, String tokenType, String withdrawAddress) {
        try {
            log.info("开始验证交易: {}, 期望数量: {}, 代币类型: {}", transactionHash, expectedAmount, tokenType);

            LambdaQueryWrapper<TokenTransferLog> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(TokenTransferLog::getHash, transactionHash);
            TokenTransferLog tokenTransferLog = tokenTransferLogMapper.selectOne(lambdaQueryWrapper);

            if (tokenTransferLog == null) {
                log.error("交易记录不存在: {}", transactionHash);
                return Boolean.FALSE;
            }

            if (!"SUCCESS".equalsIgnoreCase(tokenTransferLog.getStatus())) {
                log.error("交易状态不成功: {}, 状态: {}", transactionHash, tokenTransferLog.getStatus());
                return Boolean.FALSE;
            }

            if (new BigDecimal(expectedAmount).compareTo(new BigDecimal(tokenTransferLog.getTransferValue())) != 0) {
                log.error("交易数量不匹配. 期望: {} , 实际: {} ", expectedAmount,  tokenTransferLog.getTransferValue());
                return Boolean.FALSE;
            }

            if (!tokenType.equals(tokenTransferLog.getTokenType())){
                log.error("代币类型不匹配. 期望: {} , 实际: {} ", tokenType,  tokenTransferLog.getTokenType());
                return Boolean.FALSE;
            }

            if (withdrawAddress != null && !withdrawAddress.trim().isEmpty()) {
                if (!withdrawAddress.equalsIgnoreCase(tokenTransferLog.getFromAddress())) {
                    log.error("代币转账接收方地址不匹配. 期望: {}, 实际: {}", withdrawAddress, tokenTransferLog.getFromAddress());
                    return Boolean.FALSE;
                }
                log.info("代币转账接收方地址验证通过: {}", tokenTransferLog.getFromAddress());
            }
            
            log.info("交易验证成功: {}", transactionHash);
            return Boolean.TRUE;
            
        } catch (Exception e) {
            log.error("验证交易异常: {}", e.getMessage(), e);
            return Boolean.FALSE;
        }
    }
    
    /**
     * 从交易回执中解析ERC20代币转账接收方地址
     * @param receipt 交易回执
     * @param tokenAddress 代币合约地址
     * @return 接收方地址，如果解析失败返回null
     */
    private String getTokenTransferToAddress(TransactionReceipt receipt, String tokenAddress) {
        try {
            // ERC20 Transfer事件的签名: Transfer(address indexed from, address indexed to, uint256 value)
            // 事件签名哈希: 0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef
            String transferEventSignature = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";
            
            // 使用正确的方法获取日志
            List<Log> logs = receipt.getLogs();
            if (logs == null || logs.isEmpty()) {
                log.warn("交易回执中没有日志");
                return null;
            }
            
            for (Log log : logs) {
                // 检查日志地址是否匹配代币合约地址
                if (tokenAddress.equalsIgnoreCase(log.getAddress())) {
                    // 检查事件签名是否匹配Transfer事件
                    List<String> topics = log.getTopics();
                    if (topics != null && topics.size() >= 3) {
                        if (transferEventSignature.equals(topics.get(0))) {
                            // topics[2] 是接收方地址（to）
                            String toAddress = topics.get(2);
                            if (toAddress != null && toAddress.startsWith("0x")) {
                                // 移除前导零并添加0x前缀
                                String cleanAddress = "0x" + toAddress.substring(2).replaceFirst("^0+", "");
                                if (cleanAddress.equals("0x")) {
                                    cleanAddress = "0x0";
                                }
                                return cleanAddress;
                            }
                        }
                    }
                }
            }
            
            log.warn("未找到匹配的ERC20 Transfer事件");
            return null;
            
        } catch (Exception e) {
            log.error("解析ERC20代币转账接收方地址失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 从交易回执中解析ERC20代币转账数量
     * @param receipt 交易回执
     * @param tokenAddress 代币合约地址
     * @return 转账数量（Wei单位），如果解析失败返回null
     */
    private String getTokenTransferAmount(TransactionReceipt receipt, String tokenAddress) {
        try {
            // ERC20 Transfer事件的签名: Transfer(address indexed from, address indexed to, uint256 value)
            // 事件签名哈希: 0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef
            String transferEventSignature = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";
            
            // 使用正确的方法获取日志
            List<Log> logs = receipt.getLogs();
            if (logs == null || logs.isEmpty()) {
                log.warn("交易回执中没有日志");
                return null;
            }
            
            for (Log log : logs) {
                // 检查日志地址是否匹配代币合约地址
                if (tokenAddress.equalsIgnoreCase(log.getAddress())) {
                    // 检查事件签名是否匹配Transfer事件
                    List<String> topics = log.getTopics();
                    if (topics != null && topics.size() >= 3) {
                        if (transferEventSignature.equals(topics.get(0))) {
                            // data字段包含转账数量（uint256）
                            String data = log.getData();
                            if (data != null && data.startsWith("0x")) {
                                // 移除0x前缀并转换为BigInteger
                                String hexValue = data.substring(2);
                                return hexValue;
                            }
                        }
                    }
                }
            }
            
            log.warn("未找到匹配的ERC20 Transfer事件");
            return null;
            
        } catch (Exception e) {
            log.error("解析ERC20代币转账数量失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 将代币数量转换为Wei单位
     * @param tokenAmount 代币数量（如：10000）
     * @return Wei单位（如：10000000000000000000000）
     */
    public static String convertToWei(String tokenAmount) {
        try {
            // 假设代币有18位小数
            BigInteger amount = new BigInteger(tokenAmount);
            BigInteger weiMultiplier = new BigInteger("1000000000000000000"); // 10^18
            BigInteger weiAmount = amount.multiply(weiMultiplier);
            return weiAmount.toString();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的代币数量格式: " + tokenAmount, e);
        }
    }
    
    /**
     * 将Wei单位转换为代币数量
     * @param weiAmount Wei单位
     * @return 代币数量
     */
    public static String convertFromWei(String weiAmount) {
        try {
            BigInteger wei = new BigInteger(weiAmount);
            BigInteger tokenMultiplier = new BigInteger("1000000000000000000"); // 10^18
            BigInteger tokenAmount = wei.divide(tokenMultiplier);
            return tokenAmount.toString();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的Wei数量格式: " + weiAmount, e);
        }
    }
    
    /**
     * 比较两个数量是否相等（考虑精度问题）
     * @param actualAmount 实际数量（十六进制格式）
     * @param expectedAmount 期望数量（十进制格式）
     * @return 是否相等
     */
    private boolean isAmountEqual(String actualAmount, String expectedAmount) {
        try {
            // 将十六进制转换为BigInteger
            BigInteger actualWei = new BigInteger(actualAmount, 16);
            BigInteger expectedWei = new BigInteger(expectedAmount);
            
            // 比较Wei值
            return actualWei.equals(expectedWei);
        } catch (NumberFormatException e) {
            log.error("数量格式错误: actual={}, expected={}", actualAmount, expectedAmount);
            return false;
        }
    }
}