package com.example.eco.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

/**
 * Web3签名验证工具类
 */
@Slf4j
@Component
public class Web3SignatureUtil {
    
    /**
     * 验证Web3签名
     * @param message 原始消息
     * @param signature 签名
     * @param expectedAddress 期望的钱包地址
     * @return 验证是否通过
     */
    public boolean verifySignature(String message, String signature, String expectedAddress) {
        try {
            log.info("开始验证签名: message={}, signature={}, expectedAddress={}", message, signature, expectedAddress);
            
            // 优先尝试ethers.js标准验证方式
            boolean ethersResult = verifyEthersSignature(message, signature, expectedAddress);
            if (ethersResult) {
                log.info("ethers.js标准验证成功");
                return true;
            }
            
            // 优先尝试个人签名验证（ethers.js v6 默认使用这种方式）
            boolean personalSignResult = verifyPersonalSign(message, signature, expectedAddress);
            if (personalSignResult) {
                log.info("个人签名验证成功");
                return true;
            }
            
            // 如果消息看起来像nonce（64位十六进制字符串），使用nonce验证方式
            if (isNonceFormat(message)) {
                log.info("检测到nonce格式，使用nonce验证方式");
                boolean nonceResult = verifyNonceSignature(message, signature, expectedAddress);
                if (nonceResult) {
                    log.info("nonce签名验证成功");
                    return true;
                }
            }
            
            // 尝试原始签名
            boolean rawSignResult = verifyRawSign(message, signature, expectedAddress);
            if (rawSignResult) {
                log.info("原始签名验证成功");
                return true;
            }
            
            // 尝试直接验证签名（不添加前缀）
            boolean directSignResult = verifyDirectSign(message, signature, expectedAddress);
            if (directSignResult) {
                log.info("直接签名验证成功");
                return true;
            }
            
            log.warn("所有签名验证方法都失败");
            return false;
            
        } catch (Exception e) {
            log.error("验证Web3签名失败: message={}, signature={}, address={}", 
                    message, signature, expectedAddress, e);
            return false;
        }
    }
    
    /**
     * 验证ethers.js标准签名
     */
    private boolean verifyEthersSignature(String message, String signature, String expectedAddress) {
        try {
            log.info("尝试ethers.js标准签名验证: message={}", message);
            
            // ethers.js v6 的 signMessage 会添加前缀
            String ethMessage = "\u0019Ethereum Signed Message:\n" + message.length() + message;
            byte[] messageHash = Hash.sha3(ethMessage.getBytes(StandardCharsets.UTF_8));
            log.info("ethers.js消息: {}", ethMessage);
            log.info("ethers.js消息哈希: {}", Numeric.toHexString(messageHash));
            
            // 解析签名
            byte[] signatureBytes = Numeric.hexStringToByteArray(signature);
            if (signatureBytes.length != 65) {
                log.warn("ethers.js签名长度无效: expected=65, actual={}", signatureBytes.length);
                return false;
            }
            
            // 提取r, s, v
            byte[] r = new byte[32];
            byte[] s = new byte[32];
            System.arraycopy(signatureBytes, 0, r, 0, 32);
            System.arraycopy(signatureBytes, 32, s, 0, 32);
            byte v = signatureBytes[64];
            
            log.info("ethers.js签名解析: r={}, s={}, v={}", 
                    Numeric.toHexString(r), Numeric.toHexString(s), v);
            
            // 创建签名对象
            ECDSASignature ecdsaSignature = new ECDSASignature(
                    new BigInteger(1, r),
                    new BigInteger(1, s)
            );
            
            // 尝试恢复公钥 - 使用更简单的方法
            BigInteger publicKey = null;
            
            // 尝试v=27和v=28
            for (byte recoveryId = 0; recoveryId < 4; recoveryId++) {
                try {
                    publicKey = Sign.recoverFromSignature(recoveryId, ecdsaSignature, messageHash);
                    if (publicKey != null) {
                        String recoveredAddress = "0x" + Keys.getAddress(publicKey);
                        log.info("ethers.js恢复地址: {}, 期望地址: {}", recoveredAddress, expectedAddress);
                        
                        if (recoveredAddress.equalsIgnoreCase(expectedAddress)) {
                            log.info("ethers.js签名验证成功, recoveryId={}", recoveryId);
                            return true;
                        }
                    }
                } catch (Exception e) {
                    log.debug("ethers.js recoveryId={}失败: {}", recoveryId, e.getMessage());
                }
            }
            
            log.warn("ethers.js所有recoveryId都失败");
            return false;
            
        } catch (Exception e) {
            log.error("ethers.js签名验证失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 验证个人签名 (personal_sign)
     */
    private boolean verifyPersonalSign(String message, String signature, String expectedAddress) {
        try {
            log.info("尝试个人签名验证: message={}", message);
            
            // 1. 将消息转换为以太坊个人签名格式
            String ethMessage = "\u0019Ethereum Signed Message:\n" + message.length() + message;
            byte[] messageHash = Hash.sha3(ethMessage.getBytes(StandardCharsets.UTF_8));
            log.info("以太坊个人签名消息: {}", ethMessage);
            log.info("消息哈希: {}", Numeric.toHexString(messageHash));
            
            return verifySignatureInternal(messageHash, signature, expectedAddress, "个人签名");
            
        } catch (Exception e) {
            log.error("个人签名验证失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 验证原始签名
     */
    private boolean verifyRawSign(String message, String signature, String expectedAddress) {
        try {
            log.debug("尝试原始签名验证");
            
            // 1. 直接对消息进行哈希
            byte[] messageHash = Hash.sha3(message.getBytes(StandardCharsets.UTF_8));
            log.debug("原始消息: {}", message);
            log.debug("消息哈希: {}", Numeric.toHexString(messageHash));
            
            return verifySignatureInternal(messageHash, signature, expectedAddress, "原始签名");
            
        } catch (Exception e) {
            log.debug("原始签名验证失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 内部签名验证逻辑
     */
    private boolean verifySignatureInternal(byte[] messageHash, String signature, String expectedAddress, String signType) {
        try {
            log.info("{}开始内部验证: messageHash={}, signature={}, expectedAddress={}", 
                    signType, Numeric.toHexString(messageHash), signature, expectedAddress);
            
            // 2. 解析签名
            byte[] signatureBytes = Numeric.hexStringToByteArray(signature);
            if (signatureBytes.length != 65) {
                log.warn("{}签名长度无效: expected=65, actual={}", signType, signatureBytes.length);
                return false;
            }
            
            // 3. 提取r, s, v
            byte[] r = new byte[32];
            byte[] s = new byte[32];
            System.arraycopy(signatureBytes, 0, r, 0, 32);
            System.arraycopy(signatureBytes, 32, s, 0, 32);
            byte v = signatureBytes[64];
            
            log.info("{}签名解析: r={}, s={}, v={}", signType,
                    Numeric.toHexString(r), Numeric.toHexString(s), v);
            
            // 4. 恢复公钥
            ECDSASignature ecdsaSignature = new ECDSASignature(
                    new BigInteger(1, r),
                    new BigInteger(1, s)
            );
            
            // 尝试恢复公钥（v可能是27或28）
            BigInteger publicKey = null;
            
            // 尝试v=27
            try {
                publicKey = Sign.recoverFromSignature((byte) 27, ecdsaSignature, messageHash);
                log.info("{}尝试v=27恢复公钥: {}", signType, publicKey != null ? "成功" : "失败");
            } catch (Exception e) {
                log.debug("{}v=27恢复公钥失败: {}", signType, e.getMessage());
            }
            
            // 尝试v=28
            if (publicKey == null) {
                try {
                    publicKey = Sign.recoverFromSignature((byte) 28, ecdsaSignature, messageHash);
                    log.info("{}尝试v=28恢复公钥: {}", signType, publicKey != null ? "成功" : "失败");
                } catch (Exception e) {
                    log.debug("{}v=28恢复公钥失败: {}", signType, e.getMessage());
                }
            }
            
            // 尝试原始v值
            if (publicKey == null) {
                try {
                    publicKey = Sign.recoverFromSignature(v, ecdsaSignature, messageHash);
                    log.info("{}尝试原始v={}恢复公钥: {}", signType, v, publicKey != null ? "成功" : "失败");
                } catch (Exception e) {
                    log.debug("{}原始v={}恢复公钥失败: {}", signType, v, e.getMessage());
                }
            }
            
            if (publicKey == null) {
                log.warn("{}无法恢复公钥，所有v值都失败", signType);
                return false;
            }
            
            // 5. 从公钥计算地址
            String recoveredAddress = "0x" + Keys.getAddress(publicKey);
            
            // 6. 比较地址（忽略大小写）
            boolean isValid = recoveredAddress.equalsIgnoreCase(expectedAddress);
            
            log.info("{}验证结果: expected={}, recovered={}, valid={}", 
                    signType, expectedAddress, recoveredAddress, isValid);
            
            return isValid;
            
        } catch (Exception e) {
            log.error("{}内部验证失败: {}", signType, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 验证直接签名（不添加任何前缀）
     */
    private boolean verifyDirectSign(String message, String signature, String expectedAddress) {
        try {
            log.debug("尝试直接签名验证");
            
            // 1. 直接对消息进行哈希
            byte[] messageHash = Hash.sha3(message.getBytes(StandardCharsets.UTF_8));
            log.debug("直接消息: {}", message);
            log.debug("消息哈希: {}", Numeric.toHexString(messageHash));
            
            return verifySignatureInternal(messageHash, signature, expectedAddress, "直接签名");
            
        } catch (Exception e) {
            log.debug("直接签名验证失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查消息是否为nonce格式（64位十六进制字符串）
     */
    private boolean isNonceFormat(String message) {
        if (message == null || message.length() != 64) {
            return false;
        }
        return message.matches("[0-9a-fA-F]{64}");
    }
    
    /**
     * 验证nonce签名（针对只有nonce的消息）
     */
    private boolean verifyNonceSignature(String nonce, String signature, String expectedAddress) {
        try {
            log.info("尝试nonce签名验证: nonce={}, signature={}, expectedAddress={}", nonce, signature, expectedAddress);
            
            // 1. 将nonce转换为字节数组
            byte[] nonceBytes = Numeric.hexStringToByteArray(nonce);
            log.info("nonce字节: {}", Numeric.toHexString(nonceBytes));
            
            // 2. 对nonce进行哈希
            byte[] messageHash = Hash.sha3(nonceBytes);
            log.info("nonce哈希: {}", Numeric.toHexString(messageHash));
            
            return verifySignatureInternal(messageHash, signature, expectedAddress, "nonce签名");
            
        } catch (Exception e) {
            log.error("nonce签名验证失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 构建登录消息
     * @param nonce 随机数
     * @return 登录消息
     */
    public String buildLoginMessage(String nonce) {
        return "Login to MyApp. Nonce: " + nonce;
    }
    
    /**
     * 测试签名验证（用于调试）
     */
    public void testSignatureVerification() {
        // 使用您提供的实际数据
        String nonce = "9c0a0f384d148dda004d23e5b2c838b38f1bf6cf6553314d2acd898f44aade5f";
        String signature = "0x3ab5e04994fcd1ef619ed207795cccf88eb017e0668d83c869de964e545b38b927f6cdfc12fefe617521d0633e07ca1faa6f5707e4f1e670a6fa86f8e78516ce1c";
        String expectedAddress = "0x8d99cb210b7c9a25f46d7fffac380c55f6bb1ebc";
        
        log.info("=== 开始测试签名验证 ===");
        log.info("nonce: {}", nonce);
        log.info("签名: {}", signature);
        log.info("期望地址: {}", expectedAddress);
        
        // 测试nonce签名
        boolean nonceResult = verifyNonceSignature(nonce, signature, expectedAddress);
        log.info("nonce签名结果: {}", nonceResult);
        
        // 测试个人签名
        String message = "Login to MyApp. Nonce: " + nonce;
        boolean personalResult = verifyPersonalSign(message, signature, expectedAddress);
        log.info("个人签名结果: {}", personalResult);
        
        // 测试原始签名
        boolean rawResult = verifyRawSign(message, signature, expectedAddress);
        log.info("原始签名结果: {}", rawResult);
        
        // 测试直接签名
        boolean directResult = verifyDirectSign(message, signature, expectedAddress);
        log.info("直接签名结果: {}", directResult);
        
        log.info("=== 测试完成 ===");
    }
}
