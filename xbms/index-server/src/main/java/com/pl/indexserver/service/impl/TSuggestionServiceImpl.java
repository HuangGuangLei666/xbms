package com.pl.indexserver.service.impl;

import com.pl.indexserver.service.TSuggestionService;
import com.pl.mapper.TSuggestionMapper;
import com.pl.model.wx.TSuggestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author HuangGuangLei
 * @Date 2019/11/12
 */
@Service
public class TSuggestionServiceImpl implements TSuggestionService {

    @Autowired
    private TSuggestionMapper tSuggestionMapper;

    @Override
    public int insertSuggestion(TSuggestion suggestion) {
        return tSuggestionMapper.insertSuggestion(suggestion);
    }
}
