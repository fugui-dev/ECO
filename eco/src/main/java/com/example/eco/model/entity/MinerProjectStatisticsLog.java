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
@TableName("miner_project_statistics_log")
public class MinerProjectStatisticsLog {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 矿机项目ID
     */
    private Integer minerProjectId;

    /**
     * 消耗ESG数量
     */
    private String esgNumber;

    /**
     * 金额
     */
    private String amount;

    /**
     * 矿机限额
     */
    private String quota;

    /**
     * 日期
     */
    private String dayTime;
}
