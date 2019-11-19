package com.pl.mapper;


import com.pl.model.wx.OrderHistory;
import com.pl.model.wx.TOrder;

import java.util.List;

public interface TOrderMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TOrder record);

    int insertSelective(TOrder record);

    TOrder selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TOrder record);

    int updateByPrimaryKey(TOrder record);

    int addOrder(TOrder tOrder);

    void updateOrderStatus(String ordersSn);

    TOrder selectByTradeNo(String ordersSn);

    List<OrderHistory> selectOrderHistoryByOpenid(String openid);

    List<OrderHistory> selectCodeOrderByOpenid(String openid);
}