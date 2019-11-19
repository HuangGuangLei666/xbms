package com.pl.mapper;
import com.pl.model.wx.TUserinfo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface TUserinfoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TUserinfo record);

    int insertSelective(TUserinfo record);

    TUserinfo selectByPrimaryKey(String openid);

    int updateByPrimaryKeySelective(int id);

    int updateByPrimaryKey(@Param("id") Integer id,
                           @Param("phoneNumber")String phoneNumber,
                           @Param("recommenderId") Integer recommenderId,
                           @Param("sonIds") String sonIds);

    TUserinfo selectUserByPhoneNumber(String phoneNumber);

    TUserinfo selectUserByOpenId(String openid);

    TUserinfo selectOpenidByUserId(Integer userId);

    TUserinfo selectUserIdByOpenId(String openId);

    int updateMyStatus(@Param("userId")Integer userId, @Param("id")Integer id);

    int cancelMyStatus(@Param("userId")Integer userId,@Param("id")Integer id);

    List<TUserinfo> selectByRecommenderId(Integer recommenderId);

    List<TUserinfo> selectBySonId(Integer userId);

    int updateMyVoice(@Param("userId")Integer userId, @Param("voiceId")Integer voiceId);

    int cancelMyVoice(@Param("userId")Integer userId, @Param("voiceId")Integer voiceId);

    int cancelAndUpdateMyStatus(@Param("userId")Integer userId, @Param("id")Integer id);

    void updateMemberInfo(@Param("openid")String openid,@Param("useDays")Integer useDays);

    void updateMemberinfo(@Param("openid")String openid,@Param("useDays")Integer useDays);

    void updateEmpByOpenid(@Param("recommenderId")Integer recommenderId,
                           @Param("sonId")String sonId,
                           @Param("empNum")String empNum,
                           @Param("openid")String openid);

    TUserinfo selectByCode(String code);

    void updateIdeByOpenid(String openId);

    List<TUserinfo> selectMembershipExpireTime();

    void updateByExpireTime(Date expireTime);
}