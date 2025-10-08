package com.example.eco.api.admin;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.RecommendStatisticsLogListQry;
import com.example.eco.bean.cmd.RecommendStatisticsLogQry;
import com.example.eco.bean.dto.RecommendStatisticsLogDTO;
import com.example.eco.bean.dto.ComputingPowerDTO;
import com.example.eco.core.service.RecommendStatisticsLogService;
import com.example.eco.core.service.ComputingPowerService;
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
    @Resource(name = "computingPowerService")
    private ComputingPowerService computingPowerService;

    /**
     * 查询推荐统计日志
     */
    @PostMapping("/info")
    SingleResponse<RecommendStatisticsLogDTO> get(@RequestBody RecommendStatisticsLogQry recommendStatisticsLogQry){

        String dayTime = recommendStatisticsLogQry.getDayTime();
        if (dayTime == null || dayTime.trim().isEmpty()) {
            dayTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        
        SingleResponse<ComputingPowerDTO> response = computingPowerService.getComputingPowerInfo(
            recommendStatisticsLogQry.getWalletAddress(), dayTime);
        
        if (response.isSuccess()) {
            RecommendStatisticsLogDTO dto = convertToOldFormat(response.getData());
            return SingleResponse.of(dto);
        }
        
        return SingleResponse.buildFailure(response.getErrMessage());
    }
    
    /**
     * 转换ComputingPowerDTO为RecommendStatisticsLogDTO
     */
    private RecommendStatisticsLogDTO convertToOldFormat(ComputingPowerDTO newDto) {
        RecommendStatisticsLogDTO dto = new RecommendStatisticsLogDTO();
        dto.setWalletAddress(newDto.getWalletAddress());
        dto.setTotalComputingPower(newDto.getSelfPower().toString());
        dto.setTotalDirectRecommendComputingPower(newDto.getDirectRecommendPower().toString());
        dto.setTotalRecommendComputingPower(newDto.getRecommendPower().toString());
        dto.setMinComputingPower(newDto.getMinPower().toString());
        dto.setMaxComputingPower(newDto.getMaxPower().toString());
        dto.setNewComputingPower(newDto.getNewPower().toString());
        dto.setDirectRecommendCount(newDto.getDirectRecommendCount());
        dto.setMaxWalletAddress(newDto.getMaxPowerWalletAddress());
        return dto;
    }

    /**
     * 列表查询推荐统计日志
     */
    @PostMapping("/list")
    MultiResponse<RecommendStatisticsLogDTO> list(@RequestBody RecommendStatisticsLogListQry recommendStatisticsLogListQry){
        return recommendStatisticsLogService.list(recommendStatisticsLogListQry);
    }
}
