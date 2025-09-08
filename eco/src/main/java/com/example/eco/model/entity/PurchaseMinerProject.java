package com.example.eco.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 购买矿机项目
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("purchase_miner_project")
public class PurchaseMinerProject {


    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 价格
     */
    private String price;

    /**
     * 矿机算力
     */
    private String computingPower;

    /**
     * 购买方式类型
     */
    private String type;

    /**
     * 创建时间
     */
    private Long createTime;
}
