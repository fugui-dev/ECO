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
@TableName("withdraw_record")
public class WithdrawRecord {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 订单号
     */
    private String order;

    /**
     * 账号类型
     */
    private String type;

    /**
     * 提现数量
     */
    private String withdrawNumber;

    /**
     * 提现时间
     */
    private Long withdrawTime;

    /**
     * 审核时间
     */
    private Long reviewTime;

    /**
     * 提现状态
     */
    private String status;

    /**
     * 审核拒绝原因
     */
    private String reason;

    /**
     * 备注
     */
    private String remark;


}
