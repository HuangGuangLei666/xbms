package com.pl.indexserver.service.impl;

import com.pl.indexserver.service.TMallService;
import com.pl.mapper.TMallMapper;
import com.pl.model.wx.TMall;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author HuangGuangLei
 * @Date 2019/11/4
 */
@Service
public class TMallServiceImpl implements TMallService {

    @Autowired
    private TMallMapper tMallMapper;

    @Override
    public List<TMall> selectAllData(Integer userId) {
        return tMallMapper.selectAllData(userId);
    }

    @Override
    public int insertlabel(TMall tMall) {
        return tMallMapper.insertlabel(tMall);
    }

    @Override
    public TMall selectById(Integer id,Integer userId) {
        return tMallMapper.selectByPrimaryKey(id,userId);
    }

    @Override
    public TMall selectByIdAndDefault(Integer id) {
        return tMallMapper.selectByIdAndDefault(id);
    }

    @Override
    public int deleteLabel(Integer id, Integer userId) {
        return tMallMapper.deleteLabel(id,userId);
    }

    @Override
    public TMall selectByIdAndUserid(Integer fatherId, Integer userId) {
        return tMallMapper.selectByIdAndUserid(fatherId,userId);
    }

    @Override
    public List<TMall> selectAllDataDefaul() {
        return tMallMapper.selectAllDataDefaul();
    }

    @Override
    public List<TMall> selectByFatherId(Integer id) {
        return tMallMapper.selectByFatherId(id);
    }

    @Override
    public void insertBaseData(Integer userId) {
        tMallMapper.insertBaseData(userId);
    }
}
