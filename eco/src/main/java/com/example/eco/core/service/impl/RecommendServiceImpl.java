package com.example.eco.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.AccountCreateCmd;
import com.example.eco.bean.cmd.RecommendCreateCmd;
import com.example.eco.bean.cmd.RecommendPageQry;
import com.example.eco.bean.cmd.RecommendQry;
import com.example.eco.bean.dto.RecommendDTO;
import com.example.eco.bean.dto.RecommendRecordDTO;
import com.example.eco.common.AccountType;
import com.example.eco.core.service.AccountService;
import com.example.eco.core.service.RecommendService;
import com.example.eco.model.entity.Account;
import com.example.eco.model.entity.Recommend;
import com.example.eco.model.entity.RecommendRecord;
import com.example.eco.model.mapper.AccountMapper;
import com.example.eco.model.mapper.RecommendMapper;
import com.example.eco.model.mapper.RecommendRecordMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class RecommendServiceImpl implements RecommendService {

    @Resource
    private RecommendMapper recommendMapper;
    @Resource
    private RecommendRecordMapper recommendRecordMapper;
    @Resource
    private AccountService accountService;

    @Override
    public SingleResponse<RecommendDTO> get(RecommendQry recommendQry) {

        LambdaQueryWrapper<Recommend> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Recommend::getWalletAddress, recommendQry.getWalletAddress());

        Recommend recommend = recommendMapper.selectOne(lambdaQueryWrapper);
        if (Objects.isNull(recommend)) {
            //不存在就创建
            RecommendCreateCmd recommendCreateCmd = new RecommendCreateCmd();
            recommendCreateCmd.setWalletAddress(recommendQry.getWalletAddress());
            recommend = create(recommendCreateCmd);
        }

        RecommendDTO recommendDTO = new RecommendDTO();
        BeanUtils.copyProperties(recommend, recommendDTO);
        return SingleResponse.of(recommendDTO);
    }

    @Override
    public MultiResponse<RecommendRecordDTO> page(RecommendPageQry recommendPageQry) {

        LambdaQueryWrapper<RecommendRecord> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(RecommendRecord::getRecommendWalletAddress, recommendPageQry.getWalletAddress());

        Page<RecommendRecord> recommendRecordPage = recommendRecordMapper.selectPage(Page.of(recommendPageQry.getPageNum(), recommendPageQry.getPageSize()), lambdaQueryWrapper);
        if (CollectionUtils.isEmpty(recommendRecordPage.getRecords())) {
            return MultiResponse.buildSuccess();
        }

        List<RecommendRecordDTO> recommendRecordDTOList = new ArrayList<>();
        for (RecommendRecord recommendRecord : recommendRecordPage.getRecords()) {
            RecommendRecordDTO recommendRecordDTO = new RecommendRecordDTO();
            BeanUtils.copyProperties(recommendRecord, recommendRecordDTO);
            recommendRecordDTOList.add(recommendRecordDTO);
        }


        return MultiResponse.of(recommendRecordDTOList, (int) recommendRecordPage.getTotal());
    }

    @Transactional
    @Override
    public SingleResponse<Void> recommend(RecommendCreateCmd recommendCreateCmd) {

        LambdaQueryWrapper<Recommend> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Recommend::getWalletAddress, recommendCreateCmd.getRecommendWalletAddress());

        //查询被推荐人信息
        Recommend recommended = recommendMapper.selectOne(lambdaQueryWrapper);
        if (Objects.isNull(recommended)) {
            //不存在就创建
            RecommendCreateCmd recommendedCreateCmd = new RecommendCreateCmd();
            recommendedCreateCmd.setWalletAddress(recommendCreateCmd.getRecommendWalletAddress());
            recommended = create(recommendedCreateCmd);
        }

        if (Objects.nonNull(recommended.getRecommendWalletAddress())) {
            return SingleResponse.buildFailure("该钱包已被推荐，不能重复推荐");
        }

        LambdaQueryWrapper<Recommend> recommendLambdaQueryWrapper = new LambdaQueryWrapper<>();
        recommendLambdaQueryWrapper.eq(Recommend::getWalletAddress, recommendCreateCmd.getWalletAddress());
        //查询推荐人信息 推荐人必须存在
        Recommend recommender = recommendMapper.selectOne(recommendLambdaQueryWrapper);
        if (Objects.isNull(recommender)) {
            return SingleResponse.buildFailure("推荐人钱包地址不存在");
        }

        String leaderRecommender = recommender.getLeaderWalletAddress();
        if (Objects.isNull(recommender.getLeaderWalletAddress())) {
            leaderRecommender = recommender.getWalletAddress();
        }
        recommended.setLeaderWalletAddress(leaderRecommender);
        recommended.setUpdateTime(System.currentTimeMillis());
        recommendMapper.updateById(recommended);

        RecommendRecord recommendRecord = new RecommendRecord();
        recommendRecord.setWalletAddress(recommendCreateCmd.getRecommendWalletAddress());
        recommendRecord.setRecommendWalletAddress(recommendCreateCmd.getWalletAddress());
        recommendRecord.setRecommendTime(System.currentTimeMillis());

        recommendRecordMapper.insert(recommendRecord);


        //更新被推荐者的下级leader
        LambdaQueryWrapper<Recommend> leaderLambdaQueryWrapper = new LambdaQueryWrapper<>();
        leaderLambdaQueryWrapper.eq(Recommend::getLeaderWalletAddress, recommendCreateCmd.getRecommendWalletAddress());

        List<Recommend> recommendList = recommendMapper.selectList(leaderLambdaQueryWrapper);
        for (Recommend recommend : recommendList) {

            recommend.setLeaderWalletAddress(leaderRecommender);
            recommend.setUpdateTime(System.currentTimeMillis());

            recommendMapper.updateById(recommend);
        }
        return SingleResponse.buildSuccess();
    }

    /**
     * 创建推荐关系
     */
    public Recommend create(RecommendCreateCmd recommendCreateCmd) {

        LambdaQueryWrapper<Recommend> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Recommend::getWalletAddress, recommendCreateCmd.getWalletAddress());

        Recommend recommend = recommendMapper.selectOne(lambdaQueryWrapper);
        if (Objects.nonNull(recommend)) {
            return recommend;
        }

        recommend = new Recommend();
        recommend.setWalletAddress(recommendCreateCmd.getWalletAddress());

        recommend.setCreateTime(System.currentTimeMillis());
        recommendMapper.insert(recommend);

        AccountCreateCmd accountCreateCmd = new AccountCreateCmd();
        accountCreateCmd.setWalletAddress(recommendCreateCmd.getWalletAddress());

        accountService.createAccount(accountCreateCmd);


        return recommend;

    }
}
