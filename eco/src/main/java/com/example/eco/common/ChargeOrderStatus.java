package com.example.eco.common;

import lombok.Getter;

@Getter
public enum ChargeOrderStatus {

    PENDING("pending", "处理中"),

    SUCCESS("success", "支付成功"),

    FAILED("failed", "支付失败");

    private String code;

    private String name;

    ChargeOrderStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static ChargeOrderStatus of(String code) {
        if (code == null) {
            return null;
        }
        for (ChargeOrderStatus status : ChargeOrderStatus.values()) {
            if (code.equals(status.code)) {
                return status;
            }
        }
        return null;
    }
}
