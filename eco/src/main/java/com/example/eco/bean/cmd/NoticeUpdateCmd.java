package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class NoticeUpdateCmd {

    private Integer id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;
}
