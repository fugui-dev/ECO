package com.example.eco.api.user;

import com.example.eco.annotation.NoJwtAuth;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.RewardLevelConfigCreateCmd;
import com.example.eco.bean.cmd.RewardLevelConfigDeleteCmd;
import com.example.eco.bean.cmd.RewardLevelConfigPageQry;
import com.example.eco.bean.cmd.RewardLevelConfigUpdateCmd;
import com.example.eco.bean.dto.RewardLevelConfigDTO;
import com.example.eco.core.service.RewardLevelConfigService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/v1/user/reward/level/config")
public class RewardLevelConfigController {

    @Resource
    private RewardLevelConfigService rewardLevelConfigService;


    /**
     * 查询奖励等级配置
     */
    @PostMapping("/page")
    MultiResponse<RewardLevelConfigDTO> page(@RequestBody RewardLevelConfigPageQry rewardLevelConfigPageQry){
        return rewardLevelConfigService.page(rewardLevelConfigPageQry);
    }
}
