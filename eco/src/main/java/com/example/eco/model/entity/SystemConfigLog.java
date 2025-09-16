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
@TableName("system_config_log")
public class SystemConfigLog {

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


    /**
     * 创建时间
     */
    private Long createTime;
}
