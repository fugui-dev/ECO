package com.example.eco.api.user;

import com.example.eco.annotation.NoJwtAuth;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.WithdrawRecordCreateCmd;
import com.example.eco.bean.cmd.WithdrawRecordDealWithCmd;
import com.example.eco.bean.cmd.WithdrawRecordPageQry;
import com.example.eco.bean.cmd.withdrawRecordCancelCmd;
import com.example.eco.bean.dto.WithdrawRecordDTO;
import com.example.eco.core.service.WithdrawRecordService;
import com.example.eco.util.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
@Slf4j
@RestController
@RequestMapping("/v1/user/withdraw/record")
public class WithdrawRecordController {

    @Resource
    private WithdrawRecordService withdrawRecordService;


    /**
     * 创建提现
     */
    @PostMapping("/create")
    SingleResponse<Void> create(@RequestBody WithdrawRecordCreateCmd withdrawRecordCreateCmd){

        // 从JWT token中获取钱包地址
        String walletAddress = UserContextUtil.getCurrentWalletAddress();
        if (walletAddress == null) {
            log.warn("获取当前用户钱包地址失败");
            return SingleResponse.buildFailure("用户未登录");
        }

        withdrawRecordCreateCmd.setWalletAddress(walletAddress);

        return withdrawRecordService.create(withdrawRecordCreateCmd);
    }


    /**
     * 取消提现
     */
    @PostMapping("/cancel")
    SingleResponse<Void> cancel(@RequestBody withdrawRecordCancelCmd withdrawRecordCancelCmd){

        // 从JWT token中获取钱包地址
        String walletAddress = UserContextUtil.getCurrentWalletAddress();
        if (walletAddress == null) {
            log.warn("获取当前用户钱包地址失败");
            return SingleResponse.buildFailure("用户未登录");
        }

        withdrawRecordCancelCmd.setWalletAddress(walletAddress);

        return withdrawRecordService.cancel(withdrawRecordCancelCmd);
    }


    /**
     * 分页查询提现记录
     */
    @PostMapping("/page")
    MultiResponse<WithdrawRecordDTO> page(@RequestBody  WithdrawRecordPageQry withdrawRecordPageQry){

//        // 从JWT token中获取钱包地址
//        String walletAddress = UserContextUtil.getCurrentWalletAddress();
//        if (walletAddress == null) {
//            log.warn("获取当前用户钱包地址失败");
//            return MultiResponse.buildFailure("400","用户未登录");
//        }
//
//        withdrawRecordPageQry.setWalletAddress(walletAddress);

        return withdrawRecordService.page(withdrawRecordPageQry);
    }
}
