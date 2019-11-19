package com.pl.indexserver.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSONObject;
import com.pl.indexserver.model.TtsProperty;
import com.pl.indexserver.service.TTSService;
import com.pl.thirdparty.api.VoiceService;
import com.pl.thirdparty.dto.Result;
import com.pl.thirdparty.enums.TtsVoiceNameEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service("ttsService")
public class TTSServiceImpl implements TTSService {

    private static final Logger logger = LoggerFactory.getLogger(TTSServiceImpl.class);

    private static final String DEFAULT_PRONUNCIATION_PEOPLE = "remale";

    @Reference(version = "${thirdparty.service.version}",
            application = "${dubbo.application.id}",timeout = 180000)
    private VoiceService voiceService;

    @Override
    public long createRecordToFTP(String content, String filePath, String fileName, TtsProperty ttsProperty) {
        long size;
        try {
            if (null == ttsProperty || StringUtils.isEmpty(ttsProperty.getPronunciationPeople())) {
                ttsProperty = new TtsProperty(DEFAULT_PRONUNCIATION_PEOPLE);
            }
            TtsVoiceNameEnum ttsVoiceNameEnum = TtsVoiceNameEnum.getTtsVoiceNameEnum(ttsProperty.getPronunciationPeople());
            Result<String> stringResult = voiceService.ttsVoice(filePath, fileName, content, ttsVoiceNameEnum);
            if (stringResult.isStatus()) {
                size = Long.valueOf(stringResult.getInfo());
            } else {
                size = 0L;
            }
            logger.info("调用tts合成服务返回结果:{}", JSONObject.toJSONString(stringResult));
        } catch (Exception e) {
            logger.info("tts合成出现异常:  ", e);
            size = 0L;
        }
        return size;
    }

}
