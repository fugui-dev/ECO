package com.example.eco.api.user;

import com.example.eco.annotation.NoJwtAuth;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.RecommendCreateCmd;
import com.example.eco.bean.cmd.RecommendPageQry;
import com.example.eco.bean.cmd.RecommendQry;
import com.example.eco.bean.dto.RecommendDTO;
import com.example.eco.bean.dto.RecommendRecordDTO;
import com.example.eco.core.service.RecommendService;
import com.example.eco.util.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/v1/user/recommend")
public class RecommendController {

    @Resource
    private RecommendService recommendService;

    /**
     * 获取推荐关系 -》 查看谁推荐了我
     */
    @PostMapping("/info")
    SingleResponse<RecommendDTO> get(@RequestBody RecommendQry recommendQry) {
//        // 从JWT token中获取钱包地址
//        String walletAddress = UserContextUtil.getCurrentWalletAddress();
//        if (walletAddress == null) {
//            log.warn("获取当前用户钱包地址失败");
//            return SingleResponse.buildFailure("用户未登录");
//        }
//
//        // 设置钱包地址到查询条件中
//        recommendQry.setWalletAddress(walletAddress);
//        log.info("获取推荐关系: walletAddress={}", walletAddress);
        
        return recommendService.get(recommendQry);
    }

    /**
     * 分页获取推荐记录 -》 查看我推荐了谁
     */
    @PostMapping("/page")
    MultiResponse<RecommendRecordDTO> page(@RequestBody RecommendPageQry recommendPageQry) {
        // 从JWT token中获取钱包地址
//        String walletAddress = UserContextUtil.getCurrentWalletAddress();
//        if (walletAddress == null) {
//            log.warn("获取当前用户钱包地址失败");
//            return MultiResponse.buildFailure("400","用户未登录");
//        }
//
//        // 设置钱包地址到查询条件中
//        recommendPageQry.setWalletAddress(walletAddress);
//        log.info("分页获取推荐记录: walletAddress={}", walletAddress);
        
        return recommendService.page(recommendPageQry);
    }

    /**
     * 创建推荐关系
     */
    @PostMapping("/create")
    SingleResponse<Void> recommend(@RequestBody RecommendCreateCmd recommendCreateCmd) {
        if (!StringUtils.hasLength(recommendCreateCmd.getRecommendCode())){
            return SingleResponse.buildFailure("推荐码不能为空");
        }

        // 从JWT token中获取钱包地址
        String walletAddress = UserContextUtil.getCurrentWalletAddress();
        if (walletAddress == null) {
            log.warn("获取当前用户钱包地址失败");
            return SingleResponse.buildFailure("用户未登录");
        }
        
        // 设置推荐人钱包地址
        recommendCreateCmd.setRecommendWalletAddress(walletAddress);
        log.info("创建推荐关系: recommendWalletAddress={}, recommendCode={}", 
                walletAddress, recommendCreateCmd.getRecommendCode());

        return recommendService.recommend(recommendCreateCmd);
    }

}
