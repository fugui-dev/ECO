package com.example.eco.bean.dto;

import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

@Data
public class AccountDTO {

    private Integer id;

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 账号类型
     */
    private String type;

    /**
     * 账号类型名称
     */
    private String typeName;

    /**
     * 账号可以积分
     */
    private String number;

    /**
     * 销售数量
     */
    private String sellNumber;

    /**
     * 销售锁定数量
     */
    private String sellLockNumber;

    /**
     * 充值数量
     */
    private String chargeNumber;

    /**
     * 充值锁定数量
     */
    private String chargeLockNumber;

    /**
     * 提现数量
     */
    private String withdrawNumber;

    /**
     * 提现锁定数量
     */
    private String withdrawLockNumber;

    /**
     * 购买数量
     */
    private String buyNumber;

    /**
     * 购买锁定数量
     */
    private String buyLockNumber;

    /**
     * 静态收益
     */
    private String staticReward;

    /**
     * 动态收益
     */
    private String dynamicReward;


    private Long createTime;

    private Long updateTime;
}
