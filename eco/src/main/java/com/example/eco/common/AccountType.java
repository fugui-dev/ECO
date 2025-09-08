package com.example.eco.common;

import lombok.Getter;

@Getter
public enum AccountType {

    ESG("ESG", "ESG账户"),

    ECO("ECO", "ECO账户");

    private String code;

    private String name;

    AccountType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static AccountType of(String code) {
        if (code == null) {
            return null;
        }
        for (AccountType type : AccountType.values()) {
            if (code.equals(type.code)) {
                return type;
            }
        }
        return null;
    }
}
