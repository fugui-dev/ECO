package com.example.eco.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推荐
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("recommend")
public class Recommend {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 推荐人钱包地址
     */
    private String recommendWalletAddress;

    /**
     * 领导钱包地址
     */
    private String leaderWalletAddress;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 更新时间
     */
    private Long updateTime;

    /**
     * 状态
     */
    private String status;


}
