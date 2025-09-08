package com.example.eco.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("system_config")
public class SystemConfig {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * key名称
     */
    private String name;

    /**
     * key值
     */
    private String value;
}
