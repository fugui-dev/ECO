package com.example.eco.bean.cmd;

import com.example.eco.bean.PageQuery;
import lombok.Data;

@Data
public class PurchaseMinerProjectPageQry extends PageQuery {
    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 购买方式
     */
    private String type;

    /**
     * 状态
     */
    private String status;


    private Long startTime;


    private Long endTime;
}
