package com.example.eco.api.user;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.MinerConfigUpdateCmd;
import com.example.eco.bean.dto.MinerConfigDTO;
import com.example.eco.core.service.MinerConfigService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/v1/user/miner/config")
public class MinerConfigController {

    @Resource
    private MinerConfigService minerConfigService;




    /**
     * 查询所有矿机配置
     */
    @PostMapping("/list")
    MultiResponse<MinerConfigDTO> list(){
        return minerConfigService.list();
    }
}
