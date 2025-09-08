package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class SystemConfigUpdateCmd {

    /**
     * key名称
     */
    private String name;

    /**
     * key值
     */
    private String value;
}
