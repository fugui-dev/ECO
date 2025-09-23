package com.example.eco.bean.dto;

import lombok.Data;

@Data
public class NoticeDTO {

    private Integer id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;


    private Long createTime;
}
