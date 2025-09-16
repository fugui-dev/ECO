package com.example.eco.api.admin;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.RecommendCreateCmd;
import com.example.eco.bean.cmd.RecommendPageQry;
import com.example.eco.bean.cmd.RecommendQry;
import com.example.eco.bean.dto.RecommendDTO;
import com.example.eco.bean.dto.RecommendRecordDTO;
import com.example.eco.core.service.RecommendService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/v1/admin/recommend")
public class AdminRecommendController {

    @Resource
    private RecommendService recommendService;


    /**
     * 获取推荐关系 -》 查看谁推荐了我
     */
    @PostMapping("/info")
    SingleResponse<RecommendDTO> get(@RequestBody RecommendQry recommendQry) {
        return recommendService.get(recommendQry);
    }

    /**
     * 分页获取推荐记录 -》 查看我推荐了谁
     */
    @PostMapping("/page")
    MultiResponse<RecommendRecordDTO> page(@RequestBody RecommendPageQry recommendPageQry) {
        return recommendService.page(recommendPageQry);
    }

    /**
     * 创建推荐关系
     */
    @PostMapping("/recommend")
    SingleResponse<Void> recommend(@RequestBody RecommendCreateCmd recommendCreateCmd) {
        return recommendService.recommend(recommendCreateCmd);
    }

}
