package com.example.eco.core.service;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.SystemConfigLogPageQry;
import com.example.eco.bean.cmd.SystemConfigUpdateCmd;
import com.example.eco.bean.dto.SystemConfigDTO;
import com.example.eco.bean.dto.SystemConfigLogDTO;

public interface SystemConfigService {

    /**
     * 更新系统配置
     */
    SingleResponse<Void> update(SystemConfigUpdateCmd systemConfigUpdateCmd);

    /**
     * 获取系统配置
     */
    MultiResponse<SystemConfigDTO> list();

    /**
     * 获取系统修改记录
     */
    MultiResponse<SystemConfigLogDTO> list(SystemConfigLogPageQry systemConfigLogPageQry);
}
