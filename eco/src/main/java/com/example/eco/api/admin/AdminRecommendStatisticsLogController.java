package com.example.eco.api.admin;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.RecommendStatisticsLogListQry;
import com.example.eco.bean.cmd.RecommendStatisticsLogQry;
import com.example.eco.bean.dto.RecommendStatisticsLogDTO;
import com.example.eco.core.service.RecommendStatisticsLogService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/v1/admin/recommend/statistics/log")
public class AdminRecommendStatisticsLogController {

    @Resource
    private RecommendStatisticsLogService recommendStatisticsLogService;

    /**
     * 查询推荐统计日志
     */
    @PostMapping("/info")
    SingleResponse<RecommendStatisticsLogDTO> get(@RequestBody RecommendStatisticsLogQry recommendStatisticsLogQry){

        recommendStatisticsLogQry.setDayTime(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        return recommendStatisticsLogService.get(recommendStatisticsLogQry);
    }

    /**
     * 列表查询推荐统计日志
     */
    @PostMapping("/list")
    MultiResponse<RecommendStatisticsLogDTO> list(@RequestBody RecommendStatisticsLogListQry recommendStatisticsLogListQry){
        return recommendStatisticsLogService.list(recommendStatisticsLogListQry);
    }
}
