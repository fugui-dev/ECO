package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.NoticeCreateCmd;
import com.example.eco.bean.cmd.NoticeDeleteCmd;
import com.example.eco.bean.cmd.NoticePageQry;
import com.example.eco.bean.cmd.NoticeUpdateCmd;
import com.example.eco.bean.dto.NoticeDTO;
import com.example.eco.core.service.NoticeService;
import com.example.eco.model.entity.Notice;
import com.example.eco.model.mapper.NoticeMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class NoticeServiceImpl implements NoticeService {

    @Resource
    private NoticeMapper noticeMapper;

    @Override
    public SingleResponse<Void> create(NoticeCreateCmd noticeCreateCmd) {

        Notice notice = new Notice();
        notice.setContent(noticeCreateCmd.getContent());
        notice.setTitle(noticeCreateCmd.getTitle());

        noticeMapper.insert(notice);

        return SingleResponse.buildSuccess();
    }

    @Override
    public SingleResponse<Void> update(NoticeUpdateCmd noticeUpdateCmd) {

        Notice notice = noticeMapper.selectById(noticeUpdateCmd.getId());
        notice.setTitle(noticeUpdateCmd.getTitle());
        notice.setContent(noticeUpdateCmd.getContent());

        noticeMapper.updateById(notice);

        return SingleResponse.buildSuccess();
    }

    @Override
    public SingleResponse<Void> delete(NoticeDeleteCmd noticeDeleteCmd) {

        noticeMapper.deleteById(noticeDeleteCmd.getId());
        return SingleResponse.buildSuccess();
    }

    @Override
    public MultiResponse<NoticeDTO> page(NoticePageQry noticePageQry) {

        Page<Notice> page = noticeMapper.selectPage(Page.of(noticePageQry.getPageNum(), noticePageQry.getPageSize()), new QueryWrapper<>());

        if (CollectionUtils.isEmpty(page.getRecords())) {
            return MultiResponse.buildSuccess();
        }

        List<NoticeDTO> list = new ArrayList<>();
        for (Notice notice : page.getRecords()) {

            NoticeDTO noticeDTO = new NoticeDTO();
            BeanUtils.copyProperties(notice, noticeDTO);

            list.add(noticeDTO);
        }
        return MultiResponse.of(list, (int) page.getTotal());
    }
}
