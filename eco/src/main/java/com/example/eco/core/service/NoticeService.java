package com.example.eco.core.service;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.NoticeCreateCmd;
import com.example.eco.bean.cmd.NoticeDeleteCmd;
import com.example.eco.bean.cmd.NoticePageQry;
import com.example.eco.bean.cmd.NoticeUpdateCmd;
import com.example.eco.bean.dto.NoticeDTO;

public interface NoticeService {

    /**
     * 创建公告
     */
    SingleResponse<Void> create(NoticeCreateCmd noticeCreateCmd);

    /**
     * 更新公告
     */
    SingleResponse<Void> update(NoticeUpdateCmd noticeUpdateCmd);

    /**
     * 删除公告
     */
    SingleResponse<Void> delete(NoticeDeleteCmd noticeDeleteCmd);

    /**
     * 公告列表
     */
    MultiResponse<NoticeDTO> page(NoticePageQry noticePageQry);
}
