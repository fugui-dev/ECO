package com.example.eco.bean.cmd;

import com.example.eco.bean.PageQuery;
import lombok.Data;

@Data
public class WithdrawRecordPageQry extends PageQuery {

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 订单
     */
    private String order;

    /**
     * 账号类型
     */
    private String type;

    /**
     * 状态
     */
    private String status;
}
