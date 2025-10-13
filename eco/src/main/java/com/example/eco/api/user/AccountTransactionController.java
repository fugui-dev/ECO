package com.example.eco.api.user;

import com.example.eco.annotation.NoJwtAuth;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.AccountTransactionPageQry;
import com.example.eco.bean.dto.AccountTransactionDTO;
import com.example.eco.core.service.AccountTransactionService;
import com.example.eco.util.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
@Slf4j
@RestController
@RequestMapping("/v1/user/account/transaction")
public class AccountTransactionController {

    @Resource
    private AccountTransactionService accountTransactionService;



    /**
     * 分页查询账户交易记录
     */
    @PostMapping("/page")
    MultiResponse<AccountTransactionDTO> page(@RequestBody AccountTransactionPageQry accountTransactionPageQry){

        // 从JWT token中获取钱包地址
        String walletAddress = UserContextUtil.getCurrentWalletAddress();
        if (walletAddress == null) {
            log.warn("获取当前用户钱包地址失败");
            return MultiResponse.buildFailure("400","用户未登录");
        }

        accountTransactionPageQry.setWalletAddress(walletAddress);
        return accountTransactionService.page(accountTransactionPageQry);
    }

}
