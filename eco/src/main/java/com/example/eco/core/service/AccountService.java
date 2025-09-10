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
    MultiResponse<AccountDTO> list(AccountQry accountQry);

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
     * 添加销售金额
     */
    SingleResponse<Void> sellNumber(AccountSellNumberCmd accountSellNumberCmd);

    /**
     * 添加锁定销售金额
     */
    SingleResponse<Void> lockSellNumber(AccountLockSellNumberCmd accountLockSellNumberCmd);

    /**
     * 添加充值金额
     */
    SingleResponse<Void> chargeNumber(AccountChargeNumberCmd accountChargeNumberCmd);

    /**
     * 添加锁定充值金额
     */
    SingleResponse<Void> lockChargeNumber(AccountLockChargeNumberCmd accountLockChargeNumberCmd);

    /**
     * 添加提现金额
     */
    SingleResponse<Void> withdrawNumber(AccountWithdrawNumberCmd accountWithdrawNumberCmd);

    /**
     * 添加锁定充值金额
     */
    SingleResponse<Void> lockWithdrawNumber(AccountLockWithdrawNumberCmd accountLockWithdrawNumberCmd);

    /**
     * 购买矿机项目扣除账户金额 不能使用购买金额
     */
    SingleResponse<Void> purchaseMinerProjectNumber(AccountDeductCmd accountDeductCmd);
}
