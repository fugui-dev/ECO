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
@TableName("recommend_statistics_log")
public class RecommendStatisticsLog {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 直接推荐人数
     */
    private Integer directRecommendCount;

    /**
     * 总算力
     */
    private String  totalComputingPower;

    /**
     * 总直接推荐算力
     */
    private String totalDirectRecommendComputingPower;

    /**
     * 总推荐算力
     */
    private String totalRecommendComputingPower;

    /**
     * 统计的日期（yyyy-MM-dd）
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
