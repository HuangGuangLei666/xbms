package com.pl.indexserver.service;

import com.pl.model.wx.TUserinfo;

import java.util.Date;
import java.util.List;

/**
 * @author HuangGuangLei
 * @Date 2019/9/25
 */
public interface TUserinfoService {
    TUserinfo selectOpenidByUserId(Integer userId);

    TUserinfo selectUserIdByOpenId(String openId);

    int updateMyStatus(Integer userId, Integer id);

    int cancelMyStatus(Integer userId, Integer id);

    List<TUserinfo> selectByRecommenderId(Integer recommenderId);

    List<TUserinfo> selectBySonId(Integer userId);

    int updateMyVoice(Integer userId, Integer voiceId);

    int cancelMyVoice(Integer userId, Integer voiceId);

    int cancelAndUpdateMyStatus(Integer userId, Integer id);

    void insert(TUserinfo user);

    void updateMemberInfo(String openid,Integer useDays);

    void updateMemberinfo(String openid,Integer useDays);

    void updateEmpByOpenid(Integer recommenderId,String sonId,String empNum,String openid);

    TUserinfo selectByCode(String code);

    void updateIdeByOpenid(String openId);

    List<TUserinfo> selectMembershipExpireTime();

    void updateByExpireTime(Date expireTime);
}
