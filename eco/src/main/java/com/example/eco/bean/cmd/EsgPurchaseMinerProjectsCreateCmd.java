package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class EsgPurchaseMinerProjectsCreateCmd {

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 矿机项目ID
     */
    private Integer minerProjectId;

}
