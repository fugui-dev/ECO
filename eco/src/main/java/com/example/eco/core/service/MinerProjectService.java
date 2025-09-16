package com.example.eco.core.service;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.bean.dto.MinerProjectDTO;

public interface MinerProjectService {

    /**
     * 创建矿机项目
     */
    SingleResponse<Void> create(MinerProjectCreateCmd minerProjectCreateCmd);

    /**
     * 更新矿机项目
     */
    SingleResponse<Void> update(MinerProjectUpdateCmd minerProjectUpdateCmd);


    /**
     * 删除矿机项目
     */
    SingleResponse<Void> delete(MinerProjectDeleteCmd minerProjectDeleteCmd);

    /**
     * 分页查询矿机项目
     */
    MultiResponse<MinerProjectDTO> page(MinerProjectPageQry minerProjectPageQry);

    /**
     * 统计
     */
    SingleResponse<Void> statistics(MinerProjectStatisticsLogCmd minerProjectStatisticsLogCmd);
}
