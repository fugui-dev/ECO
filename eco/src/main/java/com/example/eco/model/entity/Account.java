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
@TableName("account")
public class Account {

    @TableId(type = IdType.AUTO)
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
     * 提现服务费
     */
    private String serviceNumber;

    /**
     * 提现锁定服务费
     */
    private String serviceLockNumber;

    /**
     * 静态收益
     */
    private String staticReward;

    /**
     * 动态收益
     */
    private String dynamicReward;

    @Version
    private Long version;

    private Long createTime;

    private Long updateTime;
}
