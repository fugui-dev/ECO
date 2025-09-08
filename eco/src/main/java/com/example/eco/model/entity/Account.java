package com.example.eco.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
     * 数量
     */
    private String number;

    /**
     * 锁定数量
     */
    private String lockNumber;

    /**
     * 提现数量
     */
    private String withdrawNumber;

    /**
     * 购买数量
     */
    private String buyNumber;

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
