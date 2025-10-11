package com.example.eco.api.admin;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.AccountPageQry;
import com.example.eco.bean.cmd.AccountTransferCmd;
import com.example.eco.bean.dto.AccountDTO;
import com.example.eco.core.service.AccountService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/v1/admin/account")
public class AdminAccountController {

    @Resource
    private AccountService accountService;

    /**
     * 获取账户信息
     */
    @PostMapping("/list")
    MultiResponse<AccountDTO> list(@RequestBody AccountPageQry accountPageQry) {
        return accountService.list(accountPageQry);
    }

    /**
     * ECO账户转账
     */
    @PostMapping("/transfer")
    SingleResponse<Void> transfer(@RequestBody AccountTransferCmd accountTransferCmd){
        return accountService.transfer(accountTransferCmd);
    }
}
