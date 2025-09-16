package com.example.eco.api.user;

import com.alibaba.excel.util.StringUtils;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.cmd.AccountPageQry;
import com.example.eco.bean.dto.AccountDTO;
import com.example.eco.core.service.AccountService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/v1/user/account")
public class AccountController {

    @Resource
    private AccountService accountService;

    /**
     * 获取账户信息
     */
    @PostMapping("/list")
    MultiResponse<AccountDTO> list(@RequestBody AccountPageQry accountPageQry) {

        if (StringUtils.isEmpty(accountPageQry.getWalletAddress())){
            return MultiResponse.buildFailure("400","钱包地址不能为空");
        }

        return accountService.list(accountPageQry);
    }
}
