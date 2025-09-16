package com.example.eco.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推荐记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("recommend_record")
public class RecommendRecord {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 被推荐人的钱包地址
     */
    private String walletAddress;

    /**
     * 推荐人的钱包地址
     */
    private String recommendWalletAddress;

    /**
     * 推荐时间
     */
    private Long recommendTime;
}
