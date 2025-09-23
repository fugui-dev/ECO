package com.example.eco.common;

import lombok.Getter;

@Getter
public enum PurchaseMinerProjectStatus {

    DEALING("DEALING", "处理中"),

    SUCCESS("SUCCESS", "成功"),

    STOP("STOP","该矿机已达到2倍的标准，停止所有奖励"),

    FAIL("FAIL", "失败");

    private String code;

    private String name;

    PurchaseMinerProjectStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static PurchaseMinerProjectStatus of(String code) {
        if (code == null) {
            return null;
        }
        for (PurchaseMinerProjectStatus status : PurchaseMinerProjectStatus.values()) {
            if (code.equals(status.code)) {
                return status;
            }
        }
        return null;
    }
}
