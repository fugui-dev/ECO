package com.example.eco.core.service;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.RewardLevelConfigCreateCmd;
import com.example.eco.bean.cmd.RewardLevelConfigDeleteCmd;
import com.example.eco.bean.cmd.RewardLevelConfigPageQry;
import com.example.eco.bean.cmd.RewardLevelConfigUpdateCmd;
import com.example.eco.bean.dto.RewardLevelConfigDTO;

public interface RewardLevelConfigService {

    /**
     * 创建奖励等级配置
     */
    SingleResponse<Void> create(RewardLevelConfigCreateCmd rewardLevelConfigCreateCmd);

    /**
     * 更新奖励等级配置
     */
    SingleResponse<Void> update(RewardLevelConfigUpdateCmd rewardLevelConfigUpdateCmd);

    /**
     * 删除奖励等级配置
     */
    SingleResponse<Void> delete(RewardLevelConfigDeleteCmd rewardLevelConfigDeleteCmd);


    /**
     * 查询奖励等级配置
     */
    MultiResponse<RewardLevelConfigDTO> page(RewardLevelConfigPageQry rewardLevelConfigPageQry);
}
