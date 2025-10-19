package com.example.eco.api.user;

import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.dto.EsgPurchaseStatsDTO;
import com.example.eco.model.entity.MinerProject;
import com.example.eco.model.mapper.MinerProjectMapper;
import com.example.eco.util.EsgPurchaseLimitUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * ESG购买用户接口
 * 用于用户查询ESG购买状态和限制信息
 */
@Slf4j
@RestController
@RequestMapping("/user/esg-purchase")
public class EsgPurchaseController {

    @Resource
    private EsgPurchaseLimitUtil esgPurchaseLimitUtil;
    
    @Resource
    private MinerProjectMapper minerProjectMapper;

    /**
     * 检查ESG购买是否可用
     * @param minerProjectId 矿机项目ID
     * @return 是否可用
     */
    @GetMapping("/check-available")
    public SingleResponse<Boolean> checkAvailable(@RequestParam Integer minerProjectId) {
        try {
            MinerProject minerProject = minerProjectMapper.selectById(minerProjectId);
            if (minerProject == null) {
                return SingleResponse.buildFailure("矿机不存在");
            }
            
            boolean available = esgPurchaseLimitUtil.checkEsgDailyLimit(minerProject);
            return SingleResponse.of(available);
        } catch (Exception e) {
            log.error("检查ESG购买可用性失败", e);
            return SingleResponse.buildFailure("检查失败: " + e.getMessage());
        }
    }

    /**
     * 检查用户今日是否已购买ESG矿机
     * @param walletAddress 钱包地址
     * @param minerProjectId 矿机项目ID
     * @return 是否已购买
     */
    @GetMapping("/check-user-purchase")
    public SingleResponse<Boolean> checkUserPurchase(@RequestParam String walletAddress, 
                                                     @RequestParam Integer minerProjectId) {
        try {
            boolean hasPurchased = esgPurchaseLimitUtil.checkUserDailyEsgPurchase(walletAddress, minerProjectId);
            return SingleResponse.of(hasPurchased);
        } catch (Exception e) {
            log.error("检查用户ESG购买状态失败", e);
            return SingleResponse.buildFailure("检查失败: " + e.getMessage());
        }
    }

    /**
     * 获取ESG购买统计信息
     * @param minerProjectId 矿机项目ID
     * @return 统计信息
     */
    @GetMapping("/stats")
    public SingleResponse<EsgPurchaseStatsDTO> getPurchaseStats(@RequestParam Integer minerProjectId) {
        try {
            MinerProject minerProject = minerProjectMapper.selectById(minerProjectId);
            if (minerProject == null) {
                return SingleResponse.buildFailure("矿机不存在");
            }

            EsgPurchaseStatsDTO stats = new EsgPurchaseStatsDTO();
            
            if (esgPurchaseLimitUtil.isEsgRushModeEnabled(minerProject)) {
                // 开启抢购模式
                int rushLimit = esgPurchaseLimitUtil.getEsgRushLimit(minerProject);
                long todayCount = esgPurchaseLimitUtil.getTodayEsgPurchaseCount(minerProjectId);
                long remaining = rushLimit - todayCount;
                boolean available = remaining > 0;
                
                stats.setRushMode(true);
                stats.setDailyLimit(rushLimit);
                stats.setTodayCount(todayCount);
                stats.setRemaining(remaining);
                stats.setAvailable(available);
            } else {
                // 未开启抢购模式
                stats.setRushMode(false);
                stats.setDailyLimit(-1);
                stats.setTodayCount(0);
                stats.setRemaining(-1);
                stats.setAvailable(true);
            }
            
            return SingleResponse.of(stats);
        } catch (Exception e) {
            log.error("获取ESG购买统计失败", e);
            return SingleResponse.buildFailure("获取统计失败: " + e.getMessage());
        }
    }


}
