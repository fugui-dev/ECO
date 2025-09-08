package com.example.eco.common;

import lombok.Getter;

@Getter
public enum AccountTransactionStatusEnum {

    DEALING("DEALING", "处理中"),

    SUCCESS("SUCCESS", "成功"),

    FAIL("FAIL", "失败");

    private String code;

    private String name;

    AccountTransactionStatusEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static AccountTransactionStatusEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (AccountTransactionStatusEnum status : AccountTransactionStatusEnum.values()) {
            if (code.equals(status.code)) {
                return status;
            }
        }
        return null;
    }
}
