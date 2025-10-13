package com.example.eco.bean.cmd;

import lombok.Data;

/**
 * 认证验证命令
 */
@Data
public class AuthVerifyCmd {
    
    /**
     * 钱包地址
     */
    private String address;
    
    /**
     * 签名
     */
    private String signature;
}
