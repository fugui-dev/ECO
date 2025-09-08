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
    private AccountMapper accountMapper;
    @Resource
    private AccountService accountService;

    @Override
    public SingleResponse<RecommendDTO> get(RecommendQry recommendQry) {

        LambdaQueryWrapper<Recommend> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Recommend::getWalletAddress, recommendQry.getWalletAddress());

        Recommend recommend = recommendMapper.selectOne(lambdaQueryWrapper);
        if (Objects.isNull(recommend)) {
            return SingleResponse.buildFailure("钱包地址未被推荐");
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

        Recommend recommended = recommendMapper.selectOne(lambdaQueryWrapper);
        if (Objects.nonNull(recommended)) {
            return SingleResponse.buildFailure("该钱包地址已被推荐");
        }

        LambdaQueryWrapper<Recommend> recommendLambdaQueryWrapper = new LambdaQueryWrapper<>();
        recommendLambdaQueryWrapper.eq(Recommend::getWalletAddress, recommendCreateCmd.getWalletAddress());
        //查询推荐人信息
        Recommend recommender = recommendMapper.selectOne(recommendLambdaQueryWrapper);
        if (Objects.isNull(recommender)) {
            return SingleResponse.buildFailure("推荐人钱包地址不存在");
        }

        recommended = new Recommend();
        recommended.setWalletAddress(recommendCreateCmd.getRecommendWalletAddress());
        recommended.setRecommendWalletAddress(recommendCreateCmd.getWalletAddress());

        if (Objects.nonNull(recommender.getLeaderWalletAddress())) {
            recommended.setLeaderWalletAddress(recommender.getLeaderWalletAddress());
        } else {
            recommended.setLeaderWalletAddress(recommender.getWalletAddress());
        }
        recommended.setCreateTime(System.currentTimeMillis());
        recommendMapper.insert(recommended);

        RecommendRecord recommendRecord = new RecommendRecord();
        recommendRecord.setWalletAddress(recommendCreateCmd.getRecommendWalletAddress());
        recommendRecord.setRecommendWalletAddress(recommendCreateCmd.getWalletAddress());
        recommendRecord.setRecommendTime(System.currentTimeMillis());

        recommendRecordMapper.insert(recommendRecord);

        AccountCreateCmd accountCreateCmd = new AccountCreateCmd();
        accountCreateCmd.setWalletAddress(recommendCreateCmd.getRecommendWalletAddress());

        accountService.createAccount(accountCreateCmd);

        return SingleResponse.buildSuccess();
    }

    @Override
    public SingleResponse<Void> create(RecommendCreateCmd recommendCreateCmd) {

        LambdaQueryWrapper<Recommend> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Recommend::getWalletAddress, recommendCreateCmd.getWalletAddress());

        Recommend recommend = recommendMapper.selectOne(lambdaQueryWrapper);
        if (Objects.nonNull(recommend)) {
            return SingleResponse.buildFailure("该钱包地址已创建");
        }

        recommend = new Recommend();
        recommend.setWalletAddress(recommendCreateCmd.getWalletAddress());

        recommend.setCreateTime(System.currentTimeMillis());
        recommendMapper.insert(recommend);

        AccountCreateCmd accountCreateCmd = new AccountCreateCmd();
        accountCreateCmd.setWalletAddress(recommendCreateCmd.getWalletAddress());

        accountService.createAccount(accountCreateCmd);

        return SingleResponse.buildSuccess();

    }
}
