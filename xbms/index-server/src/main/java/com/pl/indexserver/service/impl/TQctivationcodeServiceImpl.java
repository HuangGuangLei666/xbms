package com.pl.indexserver.service.impl;

import com.pl.indexserver.service.TQctivationcodeService;
import com.pl.mapper.TQctivationcodeMapper;
import com.pl.model.wx.TQctivationcode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author HuangGuangLei
 * @Date 2019/11/10
 */
@Service
public class TQctivationcodeServiceImpl implements TQctivationcodeService {

    @Autowired
    private TQctivationcodeMapper tQctivationcodeMapper;

    @Override
    public void addTQctivationcode(TQctivationcode tQctivationcode) {
        tQctivationcodeMapper.addTQctivationcode(tQctivationcode);
    }

    @Override
    public TQctivationcode selectByCode(String activationCode) {
        return tQctivationcodeMapper.selectByCode(activationCode);
    }

    @Override
    public int openMembershipByCode(String code, Integer userId) {
        return tQctivationcodeMapper.openMembershipByCode(code,userId);
    }

    @Override
    public TQctivationcode selectByTradeNo(String ordersSn) {
        return tQctivationcodeMapper.selectByTradeNo(ordersSn);
    }
}
