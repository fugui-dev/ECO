package com.example.eco.core.service;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.bean.dto.EsgMinerProjectDTO;
import com.example.eco.bean.dto.MinerProjectDTO;

public interface EsgMinerProjectService {

    /**
     * 创建矿机项目
     */
    SingleResponse<Void> create(EsgMinerProjectCreateCmd esgMinerProjectCreateCmd);

    /**
     * 更新矿机项目
     */
    SingleResponse<Void> update(EsgMinerProjectUpdateCmd esgMinerProjectUpdateCmd);


    /**
     * 删除矿机项目
     */
    SingleResponse<Void> delete(MinerProjectDeleteCmd minerProjectDeleteCmd);

    /**
     * 分页查询矿机项目
     */
    MultiResponse<EsgMinerProjectDTO> page(MinerProjectPageQry minerProjectPageQry);

}
