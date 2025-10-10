package com.example.eco.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("whitelist")
public class Whitelist {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String walletAddress;
}
