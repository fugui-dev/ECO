package com.example.eco.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 矿机项目
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("miner_project")
public class MinerProject {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 价格
     */
    private String price;

    /**
     * 矿机算力
     */
    private String computingPower;

    /**
     * 矿机限额
     */
    private String quota;


    private Long createTime;


    private Long updateTime;
}
