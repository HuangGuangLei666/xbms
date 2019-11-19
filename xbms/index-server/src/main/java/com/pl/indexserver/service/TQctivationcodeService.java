package com.pl.indexserver.service;

import com.pl.model.wx.TQctivationcode;

/**
 * @author HuangGuangLei
 * @Date 2019/11/10
 */
public interface TQctivationcodeService {
    void addTQctivationcode(TQctivationcode tQctivationcode);

    TQctivationcode selectByCode(String activationCode);

    int openMembershipByCode(String code, Integer userId);

    TQctivationcode selectByTradeNo(String ordersSn);
}
