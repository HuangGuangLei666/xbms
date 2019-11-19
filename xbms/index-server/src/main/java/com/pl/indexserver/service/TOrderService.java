package com.pl.indexserver.service;

import com.pl.model.wx.OrderHistory;
import com.pl.model.wx.TOrder;

import java.util.List;

/**
 * @author HuangGuangLei
 * @Date 2019/11/10
 */
public interface TOrderService {
    int addOrder(TOrder tOrder);

    TOrder selectById(Integer goodsId);

    void updateOrderStatus(String ordersSn);

    TOrder selectByTradeNo(String ordersSn);

    List<OrderHistory> selectOrderHistoryByOpenid(String openid);

    List<OrderHistory> selectCodeOrderByOpenid(String openid);
}
