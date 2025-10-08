package com.example.eco.api.admin;

import com.example.eco.bean.SingleResponse;
import com.example.eco.core.task.AccountBalanceRecalculateScheduled;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequestMapping("/admin/account-balance")
public class AdminAccountBalanceController {

    @Resource
    private AccountBalanceRecalculateScheduled accountBalanceRecalculateScheduled;

    /**
     * 重新计算账户余额
     * @param dayTime 日期，格式：yyyy-MM-dd，重放此日期之前的所有流水
     * @return 执行结果
     */
    @PostMapping("/recalculate")
    public SingleResponse<String> recalculateAccountBalance(@RequestParam String dayTime) {
        log.info("开始重新计算账户余额，日期: {}", dayTime);
        
        try {
            // 将日期转换为时间戳
            long beforeTime = LocalDate.parse(dayTime, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            
            return accountBalanceRecalculateScheduled.recalculateAccountBalance(beforeTime);
            
        } catch (Exception e) {
            log.error("重新计算账户余额异常", e);
            return SingleResponse.buildFailure("重新计算账户余额异常: " + e.getMessage());
        }
    }

    /**
     * 重新计算账户余额（使用时间戳）
     * @param beforeTime 时间戳，重放此时间之前的所有流水
     * @return 执行结果
     */
    @PostMapping("/recalculate-by-timestamp")
    public SingleResponse<String> recalculateAccountBalanceByTimestamp(@RequestParam Long beforeTime) {
        log.info("开始重新计算账户余额，时间戳: {}", beforeTime);
        
        try {
            return accountBalanceRecalculateScheduled.recalculateAccountBalance(beforeTime);
            
        } catch (Exception e) {
            log.error("重新计算账户余额异常", e);
            return SingleResponse.buildFailure("重新计算账户余额异常: " + e.getMessage());
        }
    }
}