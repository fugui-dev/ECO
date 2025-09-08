package com.example.eco.common;

import lombok.Getter;

@Getter
public enum AccountTransactionType {

    BUY("BUY", "买入"),

    SELL("SELL", "卖出"),

    CHARGE("CHARGE", "充值"),

    WITHDRAW("WITHDRAW", "提现"),

    STATIC_REWARD("STATIC_REWARD", "静态奖励"),

    DYNAMIC_REWARD("DYNAMIC_REWARD", "动态奖励");

    private String code;

    private String name;

    AccountTransactionType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static AccountTransactionType of(String code) {
        if (code == null) {
            return null;
        }
        for (AccountTransactionType type : AccountTransactionType.values()) {
            if (code.equals(type.code)) {
                return type;
            }
        }
        return null;
    }
}
