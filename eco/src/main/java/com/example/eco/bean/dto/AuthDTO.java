package com.example.eco.bean.dto;

import lombok.Data;

/**
 * 认证相关DTO
 */
@Data
public class AuthDTO {
    
    /**
     * 钱包地址
     */
    private String address;
    
    /**
     * 随机数
     */
    private String nonce;
    
    /**
     * JWT令牌
     */
    private String token;
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 错误信息
     */
    private String error;
}
