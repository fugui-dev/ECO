package com.example.eco.bean.dto;

import lombok.Data;

@Data
public class EsgAccountDTO {

    private Integer id;

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 账号可以积分
     */
    private String number;

    /**
     * 充值数量
     */
    private String chargeNumber;

    /**
     * 充值锁定数量
     */
    private String chargeLockNumber;

    /**
     * 静态收益
     */
    private String staticReward;
}
