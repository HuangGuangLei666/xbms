package com.pl.indexserver.service.impl;

import com.pl.indexserver.service.TUserinfoService;
import com.pl.mapper.TUserinfoMapper;
import com.pl.model.wx.TUserinfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @author HuangGuangLei
 * @Date 2019/9/25
 */
@Service
public class TUserinfoServiceImpl implements TUserinfoService {

    @Autowired
    private TUserinfoMapper tUserinfoMapper;

    @Override
    public TUserinfo selectOpenidByUserId(Integer userId) {
        return tUserinfoMapper.selectOpenidByUserId(userId);
    }

    @Override
    public TUserinfo selectUserIdByOpenId(String openId) {
        return tUserinfoMapper.selectUserIdByOpenId(openId);
    }

    @Override
    public int updateMyStatus(Integer userId, Integer id) {
        return tUserinfoMapper.updateMyStatus(userId,id);
    }

    @Override
    public int cancelMyStatus(Integer userId, Integer id) {
        return tUserinfoMapper.cancelMyStatus(userId,id);
    }

    @Override
    public List<TUserinfo> selectByRecommenderId(Integer recommenderId) {
        return tUserinfoMapper.selectByRecommenderId(recommenderId);
    }

    @Override
    public List<TUserinfo> selectBySonId(Integer userId) {
        return tUserinfoMapper.selectBySonId(userId);
    }

    @Override
    public int updateMyVoice(Integer userId, Integer voiceId) {
        return tUserinfoMapper.updateMyVoice(userId,voiceId);
    }

    @Override
    public int cancelMyVoice(Integer userId, Integer voiceId) {
        return tUserinfoMapper.cancelMyVoice(userId,voiceId);
    }

    @Override
    public int cancelAndUpdateMyStatus(Integer userId, Integer id) {
        return tUserinfoMapper.cancelAndUpdateMyStatus(userId,id);
    }

    @Override
    public void insert(TUserinfo user) {
        tUserinfoMapper.insertSelective(user);
    }

    @Override
    public void updateMemberInfo(String openid,Integer useDays) {
        tUserinfoMapper.updateMemberInfo(openid,useDays);
    }

    @Override
    public void updateMemberinfo(String openid,Integer useDays) {
        tUserinfoMapper.updateMemberinfo(openid, useDays);
    }

    @Override
    public void updateEmpByOpenid(Integer recommenderId,String sonId,String empNum,String openid) {
        tUserinfoMapper.updateEmpByOpenid(recommenderId,sonId,empNum,openid);
    }

    @Override
    public TUserinfo selectByCode(String code) {
        return tUserinfoMapper.selectByCode(code);
    }

    @Override
    public void updateIdeByOpenid(String openId) {
        tUserinfoMapper.updateIdeByOpenid(openId);
    }

    @Override
    public List<TUserinfo> selectMembershipExpireTime() {
        return tUserinfoMapper.selectMembershipExpireTime();
    }

    @Override
    public void updateByExpireTime(Date expireTime) {
        tUserinfoMapper.updateByExpireTime(expireTime);
    }
}
