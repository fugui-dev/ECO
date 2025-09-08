package com.example.eco.common;

import lombok.Getter;

@Getter
public enum MinerConfigEnum {

    WITHDRAW_FEE("WITHDRAW_FEE", "提币消耗ESG比例"),

    MINER_REQUIREMENT("MINER_REQUIREMENT", "矿机挖矿算力要求");

    private String code;

    private String name;

    MinerConfigEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static MinerConfigEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (MinerConfigEnum config : MinerConfigEnum.values()) {
            if (code.equals(config.code)) {
                return config;
            }
        }
        return null;
    }
}
