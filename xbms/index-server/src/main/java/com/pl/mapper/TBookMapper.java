package com.pl.mapper;
import com.pl.model.wx.TBook;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TBookMapper {
    int deleteByPrimaryKey(String phone);

    int insert(TBook book);

    int insertSelective(TBook record);

    int updateByPrimaryKeySelective(TBook record);

    int updateByPrimaryKey(TBook record);

    int delFriendByOpenIdAndPhone(@Param("openId") String openId,@Param("phone") String phone);

    int updateFriendBookByPhone(@Param("phone")String phone, @Param("openId")String openId,
                                @Param("friendName")String friendName);

    List<TBook> qryFriendDetail(@Param("openId")String openId, @Param("phone")String phone);

    TBook qryFriendByOpenidAndPhone(@Param("openId")String openId, @Param("phone")String phone);

    List<TBook> selectFriendByOpenId(String openid);

    TBook selectFriendByPhone(String phone);

    TBook selectByPhoneAndOpenid(@Param("value")String value,
                                 @Param("openId")String openid);
}