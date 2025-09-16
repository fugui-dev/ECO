package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.RewardLevelConfigCreateCmd;
import com.example.eco.bean.cmd.RewardLevelConfigDeleteCmd;
import com.example.eco.bean.cmd.RewardLevelConfigPageQry;
import com.example.eco.bean.cmd.RewardLevelConfigUpdateCmd;
import com.example.eco.bean.dto.RewardLevelConfigDTO;
import com.example.eco.core.service.RewardLevelConfigService;
import com.example.eco.model.entity.RewardLevelConfig;
import com.example.eco.model.mapper.RewardLevelConfigMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class RewardLevelConfigServiceImpl implements RewardLevelConfigService {

    @Resource
    private RewardLevelConfigMapper rewardLevelConfigMapper;

    @Override
    public SingleResponse<Void> create(RewardLevelConfigCreateCmd rewardLevelConfigCreateCmd) {

        LambdaQueryWrapper<RewardLevelConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RewardLevelConfig::getLevel, rewardLevelConfigCreateCmd.getLevel());

        RewardLevelConfig rewardLevelConfig = rewardLevelConfigMapper.selectOne(queryWrapper);
        if (Objects.nonNull(rewardLevelConfig)) {
            return SingleResponse.buildFailure("奖励等级配置已经存在");
        }

        rewardLevelConfig = new RewardLevelConfig();
        rewardLevelConfig.setLevel(rewardLevelConfigCreateCmd.getLevel());
        rewardLevelConfig.setRewardRate(rewardLevelConfigCreateCmd.getRewardRate());
        rewardLevelConfig.setCreateTime(System.currentTimeMillis());

        rewardLevelConfigMapper.insert(rewardLevelConfig);

        return SingleResponse.buildSuccess();
    }

    @Override
    public SingleResponse<Void> update(RewardLevelConfigUpdateCmd rewardLevelConfigUpdateCmd) {

        RewardLevelConfig rewardLevelConfig = rewardLevelConfigMapper.selectById(rewardLevelConfigUpdateCmd.getId());
        if (Objects.isNull(rewardLevelConfig)) {
            return SingleResponse.buildFailure("奖励等级配置不存在");
        }

        rewardLevelConfig.setRewardRate(rewardLevelConfigUpdateCmd.getRewardRate());
        rewardLevelConfigMapper.updateById(rewardLevelConfig);
        return SingleResponse.buildSuccess();
    }

    @Override
    public SingleResponse<Void> delete(RewardLevelConfigDeleteCmd rewardLevelConfigDeleteCmd) {

        rewardLevelConfigMapper.deleteById(rewardLevelConfigDeleteCmd.getId());
        return SingleResponse.buildSuccess();
    }

    @Override
    public MultiResponse<RewardLevelConfigDTO> page(RewardLevelConfigPageQry rewardLevelConfigPageQry) {

        LambdaQueryWrapper<RewardLevelConfig> queryWrapper = new LambdaQueryWrapper<>();

        Page<RewardLevelConfig> rewardLevelConfigPage = rewardLevelConfigMapper.selectPage(Page.of(rewardLevelConfigPageQry.getPageNum(), rewardLevelConfigPageQry.getPageSize()), queryWrapper);

        if (CollectionUtils.isEmpty(rewardLevelConfigPage.getRecords())) {
            return MultiResponse.buildSuccess();
        }

        List<RewardLevelConfigDTO> rewardLevelConfigList = new ArrayList<>();

        for (RewardLevelConfig rewardLevelConfig : rewardLevelConfigPage.getRecords()) {

            RewardLevelConfigDTO rewardLevelConfigDTO = new RewardLevelConfigDTO();
            BeanUtils.copyProperties(rewardLevelConfig, rewardLevelConfigDTO);

            rewardLevelConfigList.add(rewardLevelConfigDTO);
        }

        return MultiResponse.of(rewardLevelConfigList, (int) rewardLevelConfigPage.getTotal());
    }
}
