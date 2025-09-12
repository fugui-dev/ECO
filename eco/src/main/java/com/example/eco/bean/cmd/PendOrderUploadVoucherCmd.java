package com.example.eco.bean.cmd;

import lombok.Data;

@Data
public class PendOrderUploadVoucherCmd {

    private Integer id;

    /**
     * 上传凭证
     */
    private String voucher;
}
