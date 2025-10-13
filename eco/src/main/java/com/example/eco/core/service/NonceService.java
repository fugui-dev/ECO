package com.example.eco.core.service;

/**
 * Nonce服务接口
 */
public interface NonceService {
    
    /**
     * 生成并保存nonce
     * @param address 钱包地址
     * @return nonce值
     */
    String generateAndSaveNonce(String address);
    
    /**
     * 获取并删除nonce
     * @param address 钱包地址
     * @return nonce值，如果不存在或已过期返回null
     */
    String getAndDeleteNonce(String address);
    
    /**
     * 验证nonce是否存在
     * @param address 钱包地址
     * @return 是否存在
     */
    boolean existsNonce(String address);
}
