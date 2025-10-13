package com.example.eco.api.user;

import com.example.eco.annotation.NoJwtAuth;
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
@RequestMapping("/v1/user/notice")
public class NoticeController {

    @Resource
    private NoticeService noticeService;


    /**
     * 公告列表
     */
    @PostMapping("/page")
    MultiResponse<NoticeDTO> page(@RequestBody NoticePageQry noticePageQry){
        return noticeService.page(noticePageQry);
    }
}
