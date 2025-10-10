package com.example.eco.common;

import lombok.Data;
import lombok.Getter;

@Getter
public enum AccountTransactionType {

    ADD_NUMBER("ADD_NUMBER", "增加积分"),

    DEDUCT_NUMBER("DEDUCT_NUMBER", "扣除积分"),

    BUY("BUY", "买入"),

    LOCK_BUY("LOCK_BUY", "锁定买入"),

    RELEASE_LOCK_BUY("RELEASE_LOCK_BUY", "释放锁定买入"),

    ROLLBACK_LOCK_BUY("ROLLBACK_LOCK_BUY", "回滚锁定买入"),

    SELL("SELL", "卖出"),

    LOCK_SELL("LOCK_SELL", "锁定卖出"),

    RELEASE_LOCK_SELL("RELEASE_LOCK_SELL", "释放锁定卖出"),

    ROLLBACK_LOCK_SELL("ROLLBACK_LOCK_SELL", "回滚锁定卖出"),

    CHARGE("CHARGE", "充值"),

    LOCK_CHARGE("LOCK_CHARGE", "锁定充值"),

    RELEASE_LOCK_CHARGE("RELEASE_LOCK_CHARGE", "释放锁定充值"),

    ROLLBACK_LOCK_CHARGE("ROLLBACK_LOCK_CHARGE", "回滚锁定充值"),

    WITHDRAW_SERVICE("WITHDRAW_SERVICE","提现服务费"),

    LOCK_WITHDRAW_SERVICE("LOCK_WITHDRAW_SERVICE", "锁定提现服务费"),

    RELEASE_LOCK_WITHDRAW_SERVICE("RELEASE_LOCK_WITHDRAW_SERVICE", "释放锁定提现服务费"),

    ROLLBACK_LOCK_WITHDRAW_SERVICE("ROLLBACK_LOCK_WITHDRAW_SERVICE", "回滚锁定提现服务费"),

    DEDUCT_CHARGE("DEDUCT_CHARGE", "扣除充值"),

    WITHDRAW("WITHDRAW", "提现"),

    LOCK_WITHDRAW("LOCK_WITHDRAW", "锁定提现"),

    RELEASE_LOCK_WITHDRAW("RELEASE_LOCK_WITHDRAW", "释放锁定提现"),

    ROLLBACK_LOCK_WITHDRAW("ROLLBACK_LOCK_WITHDRAW", "回滚锁定提现"),

    STATIC_REWARD("STATIC_REWARD", "静态奖励"),

    DEDUCT_STATIC_REWARD("DEDUCT_STATIC_REWARD", "扣除静态奖励"),

    DYNAMIC_REWARD("DYNAMIC_REWARD", "动态奖励"),

    DEDUCT_DYNAMIC_REWARD("DEDUCT_DYNAMIC_REWARD", "扣除动态奖励"),

    DEDUCT_REWARD_SERVICE("DEDUCT_REWARD_SERVICE", "扣除奖励服务费"),

    TRANSFER_OUT("TRANSFER_OUT", "转出"),

    TRANSFER_IN("TRANSFER_IN", "转入");

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
