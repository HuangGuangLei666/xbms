package com.pl.indexserver.service;

import com.pl.model.wx.TBook;
import com.pl.model.wx.TUserinfo;

import java.util.List;

/**
 * @author HuangGuangLei
 * @Date 2019/9/18
 */
public interface TBookService {

    int insert(TBook book);

    int delFriendByOpenIdAndPhone(String openId, String phone);

    int updateFriendBookByPhone(String phone, String openId, String friendName);

    List<TBook> qryFriendDetail(String openId, String phone);

    TBook qryFriendByOpenidAndPhone(String openId, String phone);

    List<TBook> selectFriendByOpenId(String openid);

    TBook selectFriendByPhone(String phone);

    TBook selectByPhoneAndOpenid(String value, String openid);
}
