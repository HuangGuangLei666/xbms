package com.pl.indexserver.service.impl;

import com.pl.indexserver.service.TBookService;
import com.pl.mapper.TBookMapper;
import com.pl.model.wx.TBook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author HuangGuangLei
 * @Date 2019/9/18
 */
@Service
public class TBookServiceImpl implements TBookService {

    @Autowired
    private TBookMapper tBookMapper;

    @Override
    public int insert(TBook book) {
        return tBookMapper.insert(book);
    }

    @Override
    public int delFriendByOpenIdAndPhone(String openId, String phone) {
        return tBookMapper.delFriendByOpenIdAndPhone(openId,phone);
    }

    @Override
    public int updateFriendBookByPhone(String phone, String openId, String friendName) {
        return tBookMapper.updateFriendBookByPhone(phone,openId,friendName);
    }

    @Override
    public List<TBook> qryFriendDetail(String openId, String phone) {
        return tBookMapper.qryFriendDetail(openId,phone);
    }

    @Override
    public TBook qryFriendByOpenidAndPhone(String openId, String phone) {
        return tBookMapper.qryFriendByOpenidAndPhone(openId,phone);
    }

    @Override
    public List<TBook> selectFriendByOpenId(String openid) {
        return tBookMapper.selectFriendByOpenId(openid);
    }

    @Override
    public TBook selectFriendByPhone(String phone) {
        return tBookMapper.selectFriendByPhone(phone);
    }

    @Override
    public TBook selectByPhoneAndOpenid(String value, String openid) {
        return tBookMapper.selectByPhoneAndOpenid(value,openid);
    }
}
