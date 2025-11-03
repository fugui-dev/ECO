package com.example.eco.api.user;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.cmd.MinerProjectPageQry;
import com.example.eco.bean.dto.EsgMinerProjectDTO;
import com.example.eco.core.service.EsgMinerProjectService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/v1/user/esg/miner/project")
public class EsgMinerProjectController {

    @Resource
    private EsgMinerProjectService esgMinerProjectService;


    /**
     * 分页查询矿机项目
     */
    @PostMapping("/page")
    MultiResponse<EsgMinerProjectDTO> page(@RequestBody MinerProjectPageQry minerProjectPageQry){
        minerProjectPageQry.setStatus(1);
        return esgMinerProjectService.page(minerProjectPageQry);
    }
}
