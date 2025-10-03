package com.example.eco.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.util.List;

/**
 * 交易验证工具类
 */
@Slf4j
@Component
public class TransactionVerificationUtil {

    @Resource
    private Web3j web3j;
    
    /**
     * 验证链上交易（包含接收方地址验证）
     * @param transactionHash 交易哈希
     * @param expectedAmount 期望的交易数量（代币单位，如：10000）
     * @param tokenType 代币类型 (ECO/ESG)
     * @param expectedToAddress 期望的接收方地址（可选）
     * @param expectedTokenAddress 代币合约地址
     * @return 验证结果
     */
    public Boolean verifyTransaction(String transactionHash, String expectedAmount, String tokenType, String expectedToAddress,String expectedTokenAddress) {
        try {
            log.info("开始验证交易: {}, 期望数量: {}, 代币类型: {}", transactionHash, expectedAmount, tokenType);
            
            // 1. 获取交易详情
            EthTransaction transactionResponse = web3j.ethGetTransactionByHash(transactionHash).send();
            if (transactionResponse.hasError()) {
                log.error("获取交易详情失败: {}", transactionResponse.getError().getMessage());
                return null;
            }
            
            if (!transactionResponse.getTransaction().isPresent()) {
                log.error("交易不存在: {}", transactionHash);
                return null;
            }
            
            Transaction transaction = transactionResponse.getTransaction().get();
            
            // 2. 获取交易回执
            EthGetTransactionReceipt receiptResponse = web3j.ethGetTransactionReceipt(transactionHash).send();
            if (receiptResponse.hasError()) {
                log.error("获取交易回执失败: {}", receiptResponse.getError().getMessage());
                return Boolean.FALSE;
            }
            
            if (!receiptResponse.getTransactionReceipt().isPresent()) {
                log.error("交易回执不存在: {}", transactionHash);
                return Boolean.FALSE;
            }
            
            TransactionReceipt receipt = receiptResponse.getTransactionReceipt().get();
            
            // 3. 验证交易状态
            if (!receipt.isStatusOK()) {
                log.error("交易失败: {}", transactionHash);
                return Boolean.FALSE;
            }
            
            // 对于ERC20代币，需要检查to地址是否为代币合约地址
            if (!expectedTokenAddress.equalsIgnoreCase(transaction.getTo())) {
                log.error("代币地址不匹配. 期望: {}, 实际: {}", expectedTokenAddress, transaction.getTo());
                return Boolean.FALSE;
            }
            
            // 5. 验证接收方地址（如果提供了期望地址）
            if (expectedToAddress != null && !expectedToAddress.trim().isEmpty()) {
                String actualToAddress = getTokenTransferToAddress(receipt, expectedTokenAddress);
                if (actualToAddress == null) {
                    log.error("无法从交易日志中解析代币转账接收方地址");
                    return Boolean.FALSE;
                }
                
                if (!expectedToAddress.equalsIgnoreCase(actualToAddress)) {
                    log.error("代币转账接收方地址不匹配. 期望: {}, 实际: {}", expectedToAddress, actualToAddress);
                    return Boolean.FALSE;
                }
                
                log.info("代币转账接收方地址验证通过: {}", actualToAddress);
            }
            
            // 6. 验证交易数量
            // 对于ERC20代币转账，需要从交易回执的日志中解析数量
            String actualAmount = getTokenTransferAmount(receipt, expectedTokenAddress);
            if (actualAmount == null) {
                log.error("无法从交易日志中解析代币转账数量");
                return Boolean.FALSE;
            }
            
            // 将期望的代币数量转换为Wei单位进行比较
            String expectedAmountWei = convertToWei(expectedAmount);
            
            if (!isAmountEqual(actualAmount, expectedAmountWei)) {
                log.error("交易数量不匹配. 期望: {} ({} Wei), 实际: {} Wei", expectedAmount, expectedAmountWei, actualAmount);
                return Boolean.FALSE;
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



    public static void main(String[] args) throws Exception {
        // 测试参数
        String transactionHash = "0x506ba777446fd91af057f55f66db28ec4c0025c2f711566859fa1816df7dc627";
        String expectedAmount = "10000"; // 10000 ECO
        String tokenType = "ECO";
        String expectedToAddress = "0x8BA9B3B8FE0847741E98cC6dF20e69DE12142c46"; // 期望的接收方地址
        
        // 使用转换方法将代币数量转换为Wei
        String expectedAmountWei = convertToWei(expectedAmount);
        
        System.out.println("开始测试交易验证...");
        System.out.println("交易哈希: " + transactionHash);
        System.out.println("期望数量: " + expectedAmount + " " + tokenType + " (" + expectedAmountWei + " Wei)");
        System.out.println("代币类型: " + tokenType);
//        System.out.println("ECO代币地址: " + TokenAddress.ECO_TOKEN_ADDRESS);
        
        // 测试不同的网络
        String[] networks = {
            "BSC测试网", "https://bsc-testnet.infura.io/v3/4c223b9e87754809a5d8f819a261fdb7",
            "BSC主网", "https://bsc-dataseed.binance.org/",
            "以太坊主网", "https://mainnet.infura.io/v3/4c223b9e87754809a5d8f819a261fdb7",
            "以太坊测试网", "https://sepolia.infura.io/v3/4c223b9e87754809a5d8f819a261fdb7"
        };
        for (int i = 0; i < networks.length; i += 2) {
            String networkName = networks[i];
            String rpcUrl = networks[i + 1];
            
            System.out.println("\n=== 测试 " + networkName + " ===");
            System.out.println("RPC URL: " + rpcUrl);
            
            try {
                // 创建Web3j实例
                Web3j web3j = Web3j.build(new HttpService(rpcUrl));
                
                // 创建工具类实例
                TransactionVerificationUtil util = new TransactionVerificationUtil();
                util.web3j = web3j; // 手动注入Web3j实例
                
                // 执行验证（包含接收方地址验证）
                boolean result = util.verifyTransaction(transactionHash, expectedAmount, tokenType, expectedToAddress,"");
                
                System.out.println("验证结果: " + (result ? "成功" : "失败"));
                
                if (result) {
                    System.out.println("✅ 交易在 " + networkName + " 上找到并验证成功！");
                    break; // 找到后停止测试其他网络
                }
                
            } catch (Exception e) {
                System.out.println("❌ " + networkName + " 测试失败: " + e.getMessage());
            }
        }
    }
}