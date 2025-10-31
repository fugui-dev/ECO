package com.example.eco.api.user;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.EsgAccountQry;
import com.example.eco.bean.dto.EsgAccountDTO;
import com.example.eco.core.service.EsgAccountService;
import com.example.eco.util.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/v1/user/esg/account")
public class EsgAccountController {

    @Resource
    private EsgAccountService esgAccountService;


    /**
     * 获取账户
     */
    @GetMapping("/info")
    SingleResponse<EsgAccountDTO> getAccount(){
        // 从JWT token中获取钱包地址
        String walletAddress = UserContextUtil.getCurrentWalletAddress();
        if (walletAddress == null) {
            log.warn("获取当前用户钱包地址失败");
            return SingleResponse.buildFailure("用户未登录");
        }

        EsgAccountQry esgAccountQry = new EsgAccountQry();
        esgAccountQry.setWalletAddress(walletAddress);
        return esgAccountService.getAccount(esgAccountQry);
    }
}
