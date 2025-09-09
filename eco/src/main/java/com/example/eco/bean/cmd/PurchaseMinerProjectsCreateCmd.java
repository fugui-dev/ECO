package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class PurchaseMinerProjectsCreateCmd {

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 矿机项目ID
     */
    private Integer minerProjectId;

    /**
     * 购买方式类型
     */
    private String type;
}
