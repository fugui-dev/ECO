package com.example.eco.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
@TableName("purchase_miner_buy_way")
public class PurchaseMinerBuyWay {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * key名称
     */
    @TableField(value = "`name`")
    private String name;

    /**
     * key值
     */
    @TableField(value = "`value`")
    private String value;


    private Integer status;


    /**
     * 创建时间
     */
    private Long createTime;


    private Long updateTime;
}
