package com.example.eco.api.admin;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.MinerProjectCreateCmd;
import com.example.eco.bean.cmd.MinerProjectDeleteCmd;
import com.example.eco.bean.cmd.MinerProjectPageQry;
import com.example.eco.bean.cmd.MinerProjectUpdateCmd;
import com.example.eco.bean.dto.MinerProjectDTO;
import com.example.eco.core.service.MinerProjectService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/v1/admin/miner/project")
public class AdminMinerProjectController {

    @Resource
    private MinerProjectService minerProjectService;



    /**
     * 创建矿机项目
     */
    @PostMapping("/create")
    SingleResponse<Void> create(@RequestBody MinerProjectCreateCmd minerProjectCreateCmd){
        return minerProjectService.create(minerProjectCreateCmd);
    }

    /**
     * 更新矿机项目
     */
    @PostMapping("/update")
    SingleResponse<Void> update(@RequestBody MinerProjectUpdateCmd minerProjectUpdateCmd){
        return minerProjectService.update(minerProjectUpdateCmd);
    }


    /**
     * 删除矿机项目
     */
    @PostMapping("/delete")
    SingleResponse<Void> delete(@RequestBody MinerProjectDeleteCmd minerProjectDeleteCmd){
        return minerProjectService.delete(minerProjectDeleteCmd);
    }

    /**
     * 分页查询矿机项目
     */
    @PostMapping("/page")
    MultiResponse<MinerProjectDTO> page(@RequestBody MinerProjectPageQry minerProjectPageQry){
        return minerProjectService.page(minerProjectPageQry);
    }
}
