package com.example.eco.api.user;

import com.example.eco.annotation.NoJwtAuth;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.cmd.MinerProjectPageQry;
import com.example.eco.bean.dto.MinerProjectDTO;
import com.example.eco.core.service.MinerProjectService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/v1/user/miner/project")
public class MinerProjectController {

    @Resource
    private MinerProjectService minerProjectService;


    /**
     * 分页查询矿机项目
     */
    @PostMapping("/page")
    MultiResponse<MinerProjectDTO> page(@RequestBody MinerProjectPageQry minerProjectPageQry){
        minerProjectPageQry.setStatus(1);
        return minerProjectService.page(minerProjectPageQry);
    }
}
