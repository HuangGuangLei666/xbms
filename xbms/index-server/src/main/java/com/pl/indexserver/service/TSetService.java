package com.pl.indexserver.service;

import com.pl.model.wx.TSet;

import java.util.List;

/**
 * @author HuangGuangLei
 * @Date 2019/9/26
 */
public interface TSetService {
    int updateFriendSet(Integer id, Integer userId, String type, Integer voiceId, String phone);

    int updateGroupSet(Integer id, Integer userId, String type, Integer voiceId, Integer groupId);

    TSet selectBusinessVoiceByUserIdAndValue(Integer id, String phone);

    TSet selectBusinessVoiceByUserIdOrValue(Integer id, Integer groupId);

    TSet selectTSetByIdUserIdAndType(Integer id, Integer userId, String type);

    void addTSetList(List<TSet> tSetList);

    List<TSet> selectByUserIdAndValue(Integer userId, String value);

    List<TSet> selectByUserId(Integer userId);

    List<TSet> selectByOperationId(Integer aLong);

    void delOperationRecordByOperationId(Integer operationId);
}
