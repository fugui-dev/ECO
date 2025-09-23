package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class NoticeCreateCmd {

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;
}
