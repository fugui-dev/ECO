package com.example.eco.core.service;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.RecommendCreateCmd;
import com.example.eco.bean.cmd.RecommendPageQry;
import com.example.eco.bean.cmd.RecommendQry;
import com.example.eco.bean.dto.RecommendDTO;
import com.example.eco.bean.dto.RecommendRecordDTO;

public interface RecommendService {

    /**
     * 获取推荐关系 -》 查看谁推荐了我
     */
    SingleResponse<RecommendDTO> get(RecommendQry recommendQry);

    /**
     * 分页获取推荐记录 -》 查看我推荐了谁
     */
    MultiResponse<RecommendRecordDTO> page(RecommendPageQry recommendPageQry);

    /**
     * 创建推荐关系
     */
    SingleResponse<Void> recommend(RecommendCreateCmd recommendCreateCmd);

}
