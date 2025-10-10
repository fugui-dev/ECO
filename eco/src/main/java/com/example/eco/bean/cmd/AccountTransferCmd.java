package com.example.eco.bean.cmd;

import lombok.Data;

/**
 * 账户转账命令
 */
@Data
public class AccountTransferCmd {

    /**
     * 转出钱包地址
     */
    private String fromWalletAddress;

    /**
     * 转入钱包地址
     */
    private String toWalletAddress;

    /**
     * 转账数量
     */
    private String amount;

    /**
     * 备注
     */
    private String remark;
}
