package com.example.eco.api.user;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.WithdrawRecordCreateCmd;
import com.example.eco.bean.cmd.WithdrawRecordDealWithCmd;
import com.example.eco.bean.cmd.WithdrawRecordPageQry;
import com.example.eco.bean.cmd.withdrawRecordCancelCmd;
import com.example.eco.bean.dto.WithdrawRecordDTO;
import com.example.eco.core.service.WithdrawRecordService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

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
        return withdrawRecordService.create(withdrawRecordCreateCmd);
    }


    /**
     * 取消提现
     */
    @PostMapping("/cancel")
    SingleResponse<Void> cancel(@RequestBody withdrawRecordCancelCmd withdrawRecordCancelCmd){
        return withdrawRecordService.cancel(withdrawRecordCancelCmd);
    }


    /**
     * 分页查询提现记录
     */
    @PostMapping("/page")
    MultiResponse<WithdrawRecordDTO> page(@RequestBody  WithdrawRecordPageQry withdrawRecordPageQry){
        return withdrawRecordService.page(withdrawRecordPageQry);
    }
}
