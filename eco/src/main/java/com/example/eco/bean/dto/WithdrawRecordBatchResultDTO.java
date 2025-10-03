package com.example.eco.bean.dto;

import lombok.Data;
import java.util.List;

/**
 * 批量处理提现记录结果DTO
 */
@Data
public class WithdrawRecordBatchResultDTO {
    
    /**
     * 总处理数量
     */
    private Integer totalCount;
    
    /**
     * 成功处理数量
     */
    private Integer successCount;
    
    /**
     * 失败处理数量
     */
    private Integer failureCount;
    
    /**
     * 成功处理的记录ID列表
     */
    private List<Integer> successIds;
    
    /**
     * 失败处理的记录详情
     */
    private List<FailureDetail> failureDetails;
    
    /**
     * 失败详情
     */
    @Data
    public static class FailureDetail {
        private Integer id;
        private String reason;
    }
}