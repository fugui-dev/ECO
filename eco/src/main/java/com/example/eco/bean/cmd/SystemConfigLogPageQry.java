package com.example.eco.bean.cmd;

import com.example.eco.bean.PageQuery;
import lombok.Data;

@Data
public class SystemConfigLogPageQry extends PageQuery {

    private String name;

    private Long startTime;

    private Long endTime;
}
