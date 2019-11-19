package com.pl.indexserver.service.impl;

import com.pl.indexserver.service.TOrderService;
import com.pl.mapper.TOrderMapper;
import com.pl.model.wx.OrderHistory;
import com.pl.model.wx.TOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author HuangGuangLei
 * @Date 2019/11/10
 */
@Service
public class TOrderServiceImpl implements TOrderService {

    @Autowired
    private TOrderMapper tOrderMapper;

    @Override
    public int addOrder(TOrder tOrder) {
        return tOrderMapper.addOrder(tOrder);
    }

    @Override
    public TOrder selectById(Integer goodsId) {
        return tOrderMapper.selectByPrimaryKey(goodsId);
    }

    @Override
    public void updateOrderStatus(String ordersSn) {
        tOrderMapper.updateOrderStatus(ordersSn);
    }

    @Override
    public TOrder selectByTradeNo(String ordersSn) {
        return tOrderMapper.selectByTradeNo(ordersSn);
    }

    @Override
    public List<OrderHistory> selectOrderHistoryByOpenid(String openid) {
        return tOrderMapper.selectOrderHistoryByOpenid(openid);
    }

    @Override
    public List<OrderHistory> selectCodeOrderByOpenid(String openid) {
        return tOrderMapper.selectCodeOrderByOpenid(openid);
    }
}
