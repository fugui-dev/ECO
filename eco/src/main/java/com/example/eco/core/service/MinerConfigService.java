package com.example.eco.core.service;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.MinerConfigCreateCmd;
import com.example.eco.bean.cmd.MinerConfigUpdateCmd;
import com.example.eco.bean.dto.MinerConfigDTO;

public interface MinerConfigService {

    /**
     * 更新矿机配置
     */
    SingleResponse<Void> update(MinerConfigUpdateCmd minerConfigUpdateCmd);


    /**
     * 查询所有矿机配置
     */
    MultiResponse<MinerConfigDTO> list();
}
