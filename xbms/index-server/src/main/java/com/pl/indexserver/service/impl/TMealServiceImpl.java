package com.pl.indexserver.service.impl;

import com.pl.indexserver.service.TMealService;
import com.pl.mapper.TMealMapper;
import com.pl.model.wx.TMeal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author HuangGuangLei
 * @Date 2019/11/2
 */
@Service
public class TMealServiceImpl implements TMealService {

    @Autowired
    private TMealMapper tMealMapper;

    @Override
    public List<TMeal> selectMealList() {
        return tMealMapper.selectMealList();
    }

    @Override
    public TMeal selectBygoodsId(Integer goodsId) {
        return tMealMapper.selectBygoodsId(goodsId);
    }

    @Override
    public TMeal selectByCodeMealId(String code) {
        return tMealMapper.selectByCodeMealId(code);
    }

    @Override
    public TMeal selectByTradeNo(String ordersSn) {
        return tMealMapper.selectByTradeNo(ordersSn);
    }
}
