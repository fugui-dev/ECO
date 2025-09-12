package com.example.eco.bean.cmd;

import com.example.eco.bean.PageQuery;
import lombok.Data;

@Data
public class ChargeOrderPageQry extends PageQuery {

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 类型
     */
    private String type;

    /**
     * 状态
     */
    private String status;

    /**
     * 订单号
     */
    private String order;
}
