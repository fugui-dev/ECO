package com.example.eco.core.service;

import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.bean.dto.EsgAccountDTO;

public interface EsgAccountService {

    /**
     * 获取账户
     */
    SingleResponse<EsgAccountDTO> getAccount(EsgAccountQry esgAccountQry);

    /**
     * 增加账户静态奖励
     */
    SingleResponse<Void> addStaticNumber(AccountStaticNumberCmd accountStaticNumberCmd);


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
     * 购买矿机项目扣除账户金额 不能使用购买金额
     */
    SingleResponse<Void> purchaseMinerProjectNumber(AccountDeductCmd accountDeductCmd);
}
