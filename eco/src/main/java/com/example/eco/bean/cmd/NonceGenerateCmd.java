package com.example.eco.bean.cmd;

import lombok.Data;

/**
 * 生成nonce命令
 */
@Data
public class NonceGenerateCmd {
    
    /**
     * 钱包地址
     */
    private String address;
}
