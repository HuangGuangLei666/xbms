package com.pl.indexserver.untils;

import com.pl.indexserver.service.TUserinfoService;
import com.pl.model.wx.TUserinfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author HuangGuangLei
 * @Date 2019/11/13
 */
@Component
public class QuartZXbms {

    @Autowired
    private TUserinfoService tUserinfoService;

    private static final Logger logger = LoggerFactory.getLogger(QuartZXbms.class);

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Scheduled(cron = "0 0 0 * * ?")
    private void runing() {
        logger.info("=========Scheduled task started=={}==========",dateFormat.format(new Date()));
        List<TUserinfo> tUserinfoList = tUserinfoService.selectMembershipExpireTime();
        for (TUserinfo tUserinfo : tUserinfoList) {
            Date expireTime = tUserinfo.getExpireTime();
            logger.info("========ALL=expireTime={}===========",expireTime);
            if (expireTime.before(new Date())){
                logger.info("========EXP=expireTime={}===========",expireTime);
                tUserinfoService.updateByExpireTime(expireTime);
            }
        }
    }
}
