package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class PendOrderUploadVoucherCmd {

    private Integer id;


    private String hash;

    /**
     * 转账凭证图片列表,逗号分隔
     */
    private String imageList;
}
