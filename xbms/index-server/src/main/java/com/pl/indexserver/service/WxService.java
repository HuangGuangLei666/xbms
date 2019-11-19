package com.pl.indexserver.service;

import com.pl.model.wx.TUserinfo;

import java.util.Map;


/**
 * @author HGL
 * @Date 2018/12/28
 */
public interface WxService {
	void subscribe(Map<String,String> map);
	void unsubscribe(Map<String,String> map);
	
	String getAccessToken();

    TUserinfo selectUserByPhoneNumber(String phoneNumber);

	TUserinfo selectUserByOpenId(String openid);

    int updateByPrimaryKeySelective(int id);

	int updateByPrimaryKey(Integer id, String phoneNumber, Integer recommenderId, String sonIds);

    String getMediaId();
}
