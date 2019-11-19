package com.pl.mapper;


import com.pl.model.wx.TQctivationcode;
import org.apache.ibatis.annotations.Param;

public interface TQctivationcodeMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TQctivationcode record);

    int insertSelective(TQctivationcode record);

    TQctivationcode selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TQctivationcode record);

    int updateByPrimaryKey(TQctivationcode record);

    void addTQctivationcode(TQctivationcode tQctivationcode);

    TQctivationcode selectByCode(String activationCode);

    int openMembershipByCode(@Param("code") String code, @Param("userId")Integer userId);

    TQctivationcode selectByTradeNo(String ordersSn);
}