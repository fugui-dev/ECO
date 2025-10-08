package com.example.eco.common;

import lombok.Getter;

@Getter
public enum PurchaseMinerType {

    ESG("ESG", "100%使用ESC购买"),

    ECO("ECO", "100%ECO购买"),

    ECO_ESG("ECO_ESG", "50%ESC+50%ECO购买"),

    AIRDROP("AIRDROP","空投");

    private String code;

    private String name;

    PurchaseMinerType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static PurchaseMinerType of(String code) {
        if (code == null) {
            return null;
        }
        for (PurchaseMinerType type : PurchaseMinerType.values()) {
            if (code.equals(type.code)) {
                return type;
            }
        }
        return null;
    }
}
