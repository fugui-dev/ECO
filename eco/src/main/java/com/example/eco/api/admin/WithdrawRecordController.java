package com.example.eco.api.admin;

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
     * 分页查询提现记录
     */
    @PostMapping("/page")
    MultiResponse<WithdrawRecordDTO> page(@RequestBody WithdrawRecordPageQry withdrawRecordPageQry) {
        return withdrawRecordService.page(withdrawRecordPageQry);
    }

    /**
     * 处理提现
     */
    @PostMapping("/deal/with")
    SingleResponse<Void> dealWith(@RequestBody WithdrawRecordDealWithCmd withdrawRecordDealWithCmd) {
        return withdrawRecordService.dealWith(withdrawRecordDealWithCmd);
    }
}
