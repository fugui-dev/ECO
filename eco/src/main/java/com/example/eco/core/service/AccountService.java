package com.example.eco.core.service;

import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.AccountCreateCmd;

public interface AccountService {

    SingleResponse<Void> createAccount(AccountCreateCmd accountCreateCmd);
}
