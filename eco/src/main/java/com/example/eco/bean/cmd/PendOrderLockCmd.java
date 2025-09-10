package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class PendOrderLockCmd {

    /**
     * 订单号
     */
    private String order;

    /**
     * 锁定钱包地址
     */
    private String walletAddress;


}
