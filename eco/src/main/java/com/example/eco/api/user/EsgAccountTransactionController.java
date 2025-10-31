package com.example.eco.api.user;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.cmd.AccountTransactionPageQry;
import com.example.eco.bean.dto.EsgAccountTransactionDTO;
import com.example.eco.core.service.EsgAccountTransactionService;
import com.example.eco.util.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/v1/user/esg/account/transaction")
public class EsgAccountTransactionController {


    @Resource
    private EsgAccountTransactionService esgAccountTransactionService;


    /**
     * 分页查询账户交易记录
     */
    @PostMapping("/page")
    MultiResponse<EsgAccountTransactionDTO> page(@RequestBody AccountTransactionPageQry accountTransactionPageQry){

        // 从JWT token中获取钱包地址
        String walletAddress = UserContextUtil.getCurrentWalletAddress();
        if (walletAddress == null) {
            log.warn("获取当前用户钱包地址失败");
            return MultiResponse.buildFailure("400","用户未登录");
        }
        accountTransactionPageQry.setWalletAddress(walletAddress);

        return esgAccountTransactionService.page(accountTransactionPageQry);
    }
}
