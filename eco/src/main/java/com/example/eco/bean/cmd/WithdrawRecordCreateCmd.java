package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class WithdrawRecordCreateCmd {

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 账号类型
     */
    private String type;

    /**
     * 数量
     */
    private String number;

    /**
     * 备注
     */
    private String remark;
}
