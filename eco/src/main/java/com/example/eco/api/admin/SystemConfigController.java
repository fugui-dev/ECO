package com.example.eco.api.admin;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.SystemConfigUpdateCmd;
import com.example.eco.bean.dto.SystemConfigDTO;
import com.example.eco.core.service.SystemConfigService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/v1/admin/system/config")
public class SystemConfigController {

    @Resource
    private SystemConfigService systemConfigService;


    /**
     * 更新系统配置
     */
    @PostMapping("/update")
    SingleResponse<Void> update(@RequestBody SystemConfigUpdateCmd systemConfigUpdateCmd){
        return systemConfigService.update(systemConfigUpdateCmd);
    }

    /**
     * 获取系统配置
     */
    @PostMapping("/list")
    MultiResponse<SystemConfigDTO> list(){
        return systemConfigService.list();
    }
}
