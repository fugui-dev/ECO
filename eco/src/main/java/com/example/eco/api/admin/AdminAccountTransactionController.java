package com.example.eco.api.admin;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.cmd.AccountTransactionPageQry;
import com.example.eco.bean.dto.AccountTransactionDTO;
import com.example.eco.core.service.AccountTransactionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/v1/admin/account/transaction")
public class AdminAccountTransactionController {

    @Resource
    private AccountTransactionService accountTransactionService;



    /**
     * 分页查询账户交易记录
     */
    @PostMapping("/page")
    MultiResponse<AccountTransactionDTO> page(@RequestBody AccountTransactionPageQry accountTransactionPageQry){
        return accountTransactionService.page(accountTransactionPageQry);
    }

}
