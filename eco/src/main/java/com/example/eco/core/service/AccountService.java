package com.example.eco.core.service;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.bean.dto.AccountDTO;

public interface AccountService {

    /**
     * 创建账户
     */
    SingleResponse<Void> createAccount(AccountCreateCmd accountCreateCmd);

    /**
     * 获取账户信息
     */
    MultiResponse<AccountDTO> list(AccountPageQry accountPageQry);

    /**
     * 增加账户静态奖励
     */
    SingleResponse<Void> addStaticNumber(AccountStaticNumberCmd accountStaticNumberCmd);

    /**
     * 增加账户动态奖励
     */
    SingleResponse<Void> addDynamicNumber(AccountDynamicNumberCmd accountDynamicNumberCmd);

    /**
     * 购买金额
     */
    SingleResponse<Void> buyNumber(AccountBuyNumberCmd accountBuyNumberCmd);

    /**
     * 释放购买金额
     */
    SingleResponse<Void> releaseLockBuyNumber(AccountReleaseLockBuyNumberCmd accountReleaseLockBuyNumberCmd);

    /**
     * 回滚锁定购买金额
     */
    SingleResponse<Void> rollbackLockBuyNumber(RollbackLockBuyNumberCmd rollbackLockBuyNumberCmd);

    /**
     * 添加销售金额
     */
    SingleResponse<Void> sellNumber(AccountSellNumberCmd accountSellNumberCmd);

    /**
     * 释放锁定销售金额
     */
    SingleResponse<Void> releaseLockSellNumber(AccountReleaseLockSellNumberCmd accountReleaseLockSellNumberCmd);

    /**
     * 回滚锁定销售金额
     */
    SingleResponse<Void> rollbackLockSellNumber(RollbackLockSellNumberCmd rollbackLockSellNumberCmd);

    /**
     * 添加充值金额
     */
    SingleResponse<Void> chargeNumber(AccountChargeNumberCmd accountChargeNumberCmd);

    /**
     * 释放锁定充值金额
     */
    SingleResponse<Void> releaseLockChargeNumber(AccountLockChargeNumberCmd accountLockChargeNumberCmd);

    /**
     * 回滚锁定充值金额
     */
    SingleResponse<Void> rollbackLockChargeNumber(RollbackLockChargeNumberCmd rollbackLockChargeNumberCmd);

    /**
     * 添加提现金额
     */
    SingleResponse<Void> withdrawNumber(AccountWithdrawNumberCmd accountWithdrawNumberCmd);

    /**
     * 回滚锁定提现金额
     */
    SingleResponse<Void> rollbackLockWithdrawNumber(RollbackLockWithdrawNumberCmd rollbackLockWithdrawNumberCmd);

    /**
     * 释放锁定充值金额
     */
    SingleResponse<Void> releaseLockWithdrawNumber(AccountReleaseLockWithdrawNumberCmd accountReleaseLockWithdrawNumberCmd);

    /**
     * 购买矿机项目扣除账户金额 不能使用购买金额
     */
    SingleResponse<Void> purchaseMinerProjectNumber(AccountDeductCmd accountDeductCmd);


    /**
     * 回滚购买矿机项目扣除账户金额 不能使用购买金额
     */
    SingleResponse<Void> rollbackPurchaseMinerProjectNumber(AccountAddCmd accountAddCmd);


    /**
     * 添加提现服务费
     */
    SingleResponse<Void> withdrawServiceNumber(AccountWithdrawServiceCmd accountWithdrawServiceCmd);


    /**
     * 回滚提现服务费
     */
    SingleResponse<Void> rollbackLockWithdrawServiceNumber(RollbackLockWithdrawServiceCmd rollbackLockWithdrawServiceCmd);


    /**
     * 释放提现服务费
     */
    SingleResponse<Void> releaseLockWithdrawServiceNumber(AccountReleaseLockWithdrawServiceCmd accountReleaseLockWithdrawServiceCmd);


    /**
     * 扣除奖励服务费
     */
    SingleResponse<Void> rewardService(AccountDeductCmd accountDeductCmd);
}
