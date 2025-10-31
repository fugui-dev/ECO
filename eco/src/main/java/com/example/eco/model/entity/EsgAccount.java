package com.example.eco.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("esg_account")
public class EsgAccount {

    @TableId(type = IdType.AUTO)
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

    @Version
    private Long version;


    private Long createTime;

    private Long updateTime;
}
