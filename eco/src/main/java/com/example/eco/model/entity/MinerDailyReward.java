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
@TableName("miner_daily_reward")
public class MinerDailyReward {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 矿机ID
     */
    private Integer minerId;

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 矿机项目ID
     */
    private Integer minerProjectId;

    /**
     * 矿机算力
     */
    private String computingPower;

    /**
     * 总奖励数量
     */
    private String totalReward;

    /**
     * 总奖励数量价格
     */
    private String totalRewardPrice;

    /**
     * 奖励日期
     */
    private String dayTime;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 更新时间
     */
    private Long updateTime;
}
