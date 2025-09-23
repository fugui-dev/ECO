package com.example.eco.api.admin;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.NoticeCreateCmd;
import com.example.eco.bean.cmd.NoticeDeleteCmd;
import com.example.eco.bean.cmd.NoticePageQry;
import com.example.eco.bean.cmd.NoticeUpdateCmd;
import com.example.eco.bean.dto.NoticeDTO;
import com.example.eco.core.service.NoticeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/v1/admin/notice")
public class AdminNoticeController {

    @Resource
    private NoticeService noticeService;

    /**
     * 创建公告
     */
    @PostMapping("/create")
    SingleResponse<Void> create(@RequestBody NoticeCreateCmd noticeCreateCmd){
        return noticeService.create(noticeCreateCmd);
    }

    /**
     * 更新公告
     */
    @PostMapping("/update")
    SingleResponse<Void> update(@RequestBody NoticeUpdateCmd noticeUpdateCmd){
        return noticeService.update(noticeUpdateCmd);
    }

    /**
     * 删除公告
     */
    @PostMapping("/delete")
    SingleResponse<Void> delete(@RequestBody NoticeDeleteCmd noticeDeleteCmd){
        return noticeService.delete(noticeDeleteCmd);
    }

    /**
     * 公告列表
     */
    @PostMapping("/page")
    MultiResponse<NoticeDTO> page(@RequestBody NoticePageQry noticePageQry){
        return noticeService.page(noticePageQry);
    }
}
