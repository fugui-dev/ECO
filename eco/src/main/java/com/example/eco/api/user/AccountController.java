package com.example.eco.api.user;

import com.alibaba.excel.util.StringUtils;
import com.example.eco.annotation.NoJwtAuth;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.AccountPageQry;
import com.example.eco.bean.cmd.AccountTransferCmd;
import com.example.eco.bean.dto.AccountDTO;
import com.example.eco.core.service.AccountService;
import com.example.eco.util.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/v1/user/account")
public class AccountController {

    @Resource
    private AccountService accountService;

    /**
     * 首页 -》获取账户信息 （ESG ECO 数量）
     */
    @PostMapping("/list")
    MultiResponse<AccountDTO> list(@RequestBody AccountPageQry accountPageQry) {
        // 从JWT token中获取钱包地址
        String walletAddress = UserContextUtil.getCurrentWalletAddress();
        if (walletAddress == null) {
            log.warn("获取当前用户钱包地址失败");
            return MultiResponse.buildFailure("400","用户未登录");
        }
        
        // 设置钱包地址到查询条件中
        accountPageQry.setWalletAddress(walletAddress);
        log.info("获取账户信息: walletAddress={}", walletAddress);
        
        return accountService.list(accountPageQry);
    }

    /**
     * ECO账户转账
     */
    @PostMapping("/transfer")
    SingleResponse<Void> transfer(@RequestBody AccountTransferCmd accountTransferCmd) {
        // 从JWT token中获取钱包地址
        String walletAddress = UserContextUtil.getCurrentWalletAddress();
        if (walletAddress == null) {
            log.warn("获取当前用户钱包地址失败");
            return SingleResponse.buildFailure("用户未登录");
        }
        
        // 验证转入钱包地址格式
        String toWalletAddress = accountTransferCmd.getToWalletAddress();
        if (toWalletAddress == null || toWalletAddress.trim().isEmpty()) {
            log.warn("转入钱包地址为空");
            return SingleResponse.buildFailure("转入钱包地址不能为空");
        }
        
        // 验证钱包地址格式：应该是以0x开头的42位字符
        if (!toWalletAddress.startsWith("0x") || toWalletAddress.length() != 42) {
            log.warn("转入钱包地址格式错误: {}", toWalletAddress);
            return SingleResponse.buildFailure("转入钱包地址格式错误");
        }
        
        // 验证是否为有效的十六进制地址
        if (!toWalletAddress.matches("^0x[a-fA-F0-9]{40}$")) {
            log.warn("转入钱包地址不是有效的十六进制地址: {}", toWalletAddress);
            return SingleResponse.buildFailure("转入钱包地址不是有效的十六进制地址");
        }
        
        // 设置转出钱包地址
        accountTransferCmd.setFromWalletAddress(walletAddress);
        log.info("账户转账: fromWalletAddress={}, toWalletAddress={}, amount={}", 
                walletAddress, accountTransferCmd.getToWalletAddress(), accountTransferCmd.getAmount());
        
        return accountService.transfer(accountTransferCmd);
    }
}
