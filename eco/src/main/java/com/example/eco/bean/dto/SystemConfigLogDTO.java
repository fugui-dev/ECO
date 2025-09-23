package com.example.eco.bean.dto;

import lombok.Data;

@Data
public class SystemConfigLogDTO {

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
