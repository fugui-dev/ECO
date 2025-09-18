package com.example.eco.core.service.impl;

import com.alibaba.excel.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.cmd.RewardStatisticsLogPageQry;
import com.example.eco.bean.dto.RewardStatisticsLogDTO;
import com.example.eco.core.service.RewardStatisticsLogService;
import com.example.eco.model.entity.RewardStatisticsLog;
import com.example.eco.model.mapper.RewardStatisticsLogMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class RewardStatisticsLogServiceImpl implements RewardStatisticsLogService {

    @Resource
    private RewardStatisticsLogMapper rewardStatisticsLogMapper;

    @Override
    public MultiResponse<RewardStatisticsLogDTO> page(RewardStatisticsLogPageQry rewardStatisticsLogPageQry) {

        LambdaQueryWrapper<RewardStatisticsLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StringUtils.isNotBlank(rewardStatisticsLogPageQry.getDayTime()), RewardStatisticsLog::getDayTime, rewardStatisticsLogPageQry.getDayTime());

        Page<RewardStatisticsLog> statisticsLogPage = rewardStatisticsLogMapper.selectPage(Page.of(rewardStatisticsLogPageQry.getPageNum(), rewardStatisticsLogPageQry.getPageSize()), queryWrapper);

        List<RewardStatisticsLogDTO> rewardStatisticsLogList = new ArrayList<>();

        for (RewardStatisticsLog rewardStatisticsLog : statisticsLogPage.getRecords()) {

            RewardStatisticsLogDTO rewardStatisticsLogDTO = new RewardStatisticsLogDTO();
            BeanUtils.copyProperties(rewardStatisticsLog, rewardStatisticsLogDTO);

            rewardStatisticsLogList.add(rewardStatisticsLogDTO);
        }
        return MultiResponse.of(rewardStatisticsLogList, (int) statisticsLogPage.getTotal());
    }
}
