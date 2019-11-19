package com.pl.indexserver.web;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sound.midi.Soundbank;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import cn.jiguang.common.resp.APIConnectionException;
import cn.jiguang.common.resp.APIRequestException;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.wxpay.sdk.WXPayUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.pl.indexserver.model.CheckSmsCodeResp;
import com.pl.indexserver.model.SendSmsResp;
import com.pl.indexserver.service.*;
import com.pl.indexserver.untils.*;
import com.pl.indexserver.untils.jiguang.SendSMSResult;
import com.pl.indexserver.untils.jiguang.ValidSMSResult;
import com.pl.indexserver.untils.jiguang.common.SMSClient;
import com.pl.indexserver.untils.jiguang.common.model.SMSPayload;
import com.pl.model.TDialog;
import com.pl.model.TDialogDetailExt;
import com.pl.model.UserIsMembership;
import com.pl.model.messagetype.Image;
import com.pl.model.messagetype.ImageMessage;
import com.pl.model.messagetype.TextMessage;
import com.pl.model.wx.*;
import com.pl.thirdparty.api.VoiceService;
import com.pl.thirdparty.dto.Result;
import com.pl.thirdparty.dto.request.TemplateParamRequest;
import com.pl.thirdparty.enums.TtsVoiceNameEnum;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.poi.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sun.net.ftp.FtpClient;

/**
 * @author HGL
 * @Date 2018/12/28
 */
@RestController
@RequestMapping(value = "/busiManagement/wxgzh")
public class WxController {

    private static final Logger logger = LoggerFactory.getLogger(WxController.class);
    private static int seq = (int) (System.currentTimeMillis() % 100000000);

    @Autowired
    private RedisClient redisClient;
    @Autowired
    private WxService wxService;
    @Autowired
    private TBookService tBookService;
    @Autowired
    private TGoupService tGoupService;
    @Autowired
    private TContentService tContentService;
    @Autowired
    private TVoiceService tVoiceService;
    @Autowired
    private TUserVoiceService tUserVoiceService;
    @Autowired
    private TUserinfoService tUserinfoService;
    @Autowired
    private TSetService tSetService;
    @Autowired
    private TLabelService tLabelService;
    @Autowired //对话详情
    private TDialogService tDialogService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private TMealService tMealService;
    @Autowired //对话详情
    private TDialogDetailExtProxy tDialogDetailExtProxy;
    @Autowired
    private TMallService tMallService;
    @Autowired
    private TMechanismService tMechanismService;
    @Autowired
    private TOrderService tOrderService;
    @Autowired
    private TQctivationcodeService tQctivationcodeService;
    @Autowired
    private TSuggestionService tSuggestionService;
    @Reference(version = "${thirdparty.service.version}",
            application = "${dubbo.application.id}", timeout = 180000)
    private VoiceService voiceService;
    @Value("${jiguang.appkey}")
    private String appKey;
    @Value("${jiguang.mastersecret}")
    private String masterSecret;
    @Value("${xiaobingsecretary.AppId}")
    private String appId;
    @Value("${xiaobingsecretary.AppSecret}")
    private String appSecret;

    /*@Value("${xbms.orgimage}")
    private String imageUrl;*/
    //ftp
    @Value("${ftp.address}")
    private String address;
    @Value("${ftp.port}")
    private int port;
    @Value("${ftp.name}")
    private String ftpName;
    @Value("${ftp.password}")
    private String password;


    /**
     * 验证消息的确来自微信服务器
     *
     * @param signature
     * @param timestamp
     * @param nonce
     * @param echostr
     * @return
     */
    @RequestMapping(value = "/receiveMsg", method = RequestMethod.GET)
    @ResponseBody
    public String token(String signature, String timestamp, String nonce,
                        String echostr) {
        logger.info("==========signature=" + signature);

        // 通过检验signature对请求进行校验，若校验成功则原样返回echostr，表示接入成功，否则接入失败
        if (SignUtil.checkSignature(signature, timestamp, nonce)) {
            logger.info("======token====OK=====" + echostr);
            return echostr;

        }
        logger.info("========token==no OK=====");

        return "";
    }


    /**
     * 关注、取消公众号(事件)
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "/receiveMsg", method = RequestMethod.POST)
//    @ResponseBody
//    @RequestBody String req,
    public String receiveMsg(HttpServletRequest request, HttpServletResponse response) {

        logger.info("==========req=" + request);
        String message = "success";
        String respMessage = null;
        String imgMessage = null;
        ServletOutputStream out = null;
        try {
            request.setCharacterEncoding("UTF-8");
            response.setCharacterEncoding("UTF-8");
            // 把微信返回的xml信息转义成map
            /*Map<String, String> map = xmlToMap(req);*/
            Map<String, String> map = MessageUtil.xmlToMap(request);

            logger.info("微信接收到的消息为:" + map.toString());
            String fromUserName = map.get("FromUserName");// 消息来源用户标识
            String toUserName = map.get("ToUserName");// 消息目的用户标识
            String msgType = map.get("MsgType");// 消息类型(event或者text)
            logger.info("消息来源于:" + fromUserName);
            logger.info("openId:" + toUserName);
            logger.info("消息类型为:" + msgType);
            String eventType = map.get("Event");// 事件类型
            String nickName = getUserNickName(fromUserName);

            if ("event".equals(msgType)) {// 如果为事件类型
                if ("subscribe".equals(eventType)) {// 处理订阅事件
                    String content = "欢迎关注,这是一个公众号测试账号,您可以回复任意消息测试,开发者郭先生,18629374628";
                    String msg = "@" + nickName + "," + content;
                    logger.info("事件类型为:" + "," + eventType);
                    logger.info("==========subscribe=" + map);
                    wxService.subscribe(map);

                    // 订阅
                    //文本消息
                    TextMessage text = new TextMessage();
                    text.setContent("你好！我是你的电话秘书“小兵闺秘”、只要你现在拥有我。\n" +
                            "\n" +
                            "防骚扰：从此你将不再担心被电话骚扰，我还还可以发起反骚扰！\n" +
                            "\n" +
                            "防诈骗：给你分析来电人的意图，分辨屏蔽诈骗嫌疑电话！\n" +
                            "\n" +
                            "防漏接：开会，休息，坐飞机，手机没电，信号不好，再也不用担心漏接电话！不丢失重要的来电信息、保留你感兴趣的来电！\n" +
                            "\n" +
                            "不想接：我将帮您应付不想接的电话，催收，借钱，统统我搞定！\n" +
                            "\n" +
                            "不能接：当你睡觉、上课上班、上飞机、运动、不方便任何场景，为你智能接听应答。\n" +
                            "\n" +
                            "不得不接：对送餐、快递、约会、领导亲人等重要电话和必须及时接听的电话及时给你处理好！");
                    text.setToUserName(fromUserName);
                    text.setFromUserName(toUserName);
                    text.setCreateTime(new Date().getTime());
                    text.setMsgType(MessageUtil.RESP_MESSAGE_TYPE_TEXT);
                    logger.info("%%%%%%%%%%%text={}%%%%%%%%%%%%", text);

                    respMessage = MessageUtil.textMessageToXml(text);
                    logger.info("%%%%%%%%respMessage={}%%%%%%%%", respMessage);

                    //图片
                    String mediaId = wxService.getMediaId();
                    logger.info("==========mediaId={}=============", mediaId);
                    Image image = new Image();
                    ImageMessage imageMessage = new ImageMessage();
                    image.setMediaId(mediaId);
                    imageMessage.setFromUserName(toUserName);
                    imageMessage.setToUserName(fromUserName);
                    imageMessage.setCreateTime(new Date().getTime());
                    imageMessage.setImage(image);
                    imageMessage.setMsgType(MessageUtil.REQ_MESSAGE_TYPE_IMAGE);
                    imgMessage = MessageUtil.imageMessageToXml(imageMessage);
                    logger.info("%%%%%%%%imgMessage={}%%%%%%%%", imgMessage);

                    try {
                        out = response.getOutputStream();
                        out.print(respMessage);
                        out.print(respMessage);
                    } catch (IOException e) {
                        e.printStackTrace();
                        logger.error(e.getMessage(), e);
                    } finally {
                        out.close();
                        out = null;
                    }

                } else if ("unsubscribe".equals(eventType)) {// 处理取消订阅事件
                    logger.info("事件类型为:" + eventType);
                    logger.info("==========un subscribe=" + map);
                    wxService.unsubscribe(map);

                }
            } else {
                // 微信消息分为事件推送消息和普通消息,非事件即为普通消息类型
                switch (msgType) {
                    case "text": {// 文本消息
                        String content = map.get("Content");// 用户发的消息内容
                        content = "您发的消息内容是:" + content + ",如需帮助,请联系郭先生,18629374628";
                        break;
                    }
                    case "image": {// 图片消息
                        String content = "您发的消息内容是图片,如需帮助,请联系郭先生,18629374628";
                        break;
                    }
                    default: {// 其他类型的普通消息
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("==========wxController receiveMsg =", e);
        }
        logger.info("关注微信公众号自动回复的消息内容为:" + message);
        return message;
    }

    public static String getUserNickName(String openId) {
        // String nickName = null;
        // try {
        // Map map = WeiXinUtil.cacheTokenAndTicket();
        // String token = (String)map.get("access_token");
        // String url = URL.replace("OPENID", openId).replace("ACCESS_TOKEN",
        // token);
        // JSONObject obj = HttpUtil.HttpGet(url);
        // nickName = (String)obj.get("nickname");
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
        return openId;
    }

    /**
     * XML格式字符串转换为Map
     *
     * @param xml XML字符串
     * @return XML数据转换后的Map
     * @throws Exception
     */
    public static Map<String, String> xmlToMap(String xml) {
        try {
            Map<String, String> data = new HashMap<>();
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            InputStream stream = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            org.w3c.dom.Document doc = documentBuilder.parse(stream);
            doc.getDocumentElement().normalize();
            NodeList nodeList = doc.getDocumentElement().getChildNodes();
            for (int idx = 0; idx < nodeList.getLength(); ++idx) {
                Node node = nodeList.item(idx);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    org.w3c.dom.Element element = (org.w3c.dom.Element) node;
                    data.put(element.getNodeName(), element.getTextContent());
                }
            }
            stream.close();
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 发送验证码短信
     *
     * @param phoneNumber
     * @return {
     * "retCode": 0,
     * "retDesc": "验证码发送成功",
     * "rateLimitQuota": 0,
     * "rateLimitRemaining": 0,
     * "rateLimitReset": 0,
     * "responseCode": 200,
     * "originalContent": "{\"msg_id\":\"848267378766\"}",
     * "resultOK": true
     * }
     */
    @RequestMapping(value = "/sendSms.do")
    @ResponseBody
    public SendSmsResp sendSMSCode(String phoneNumber) {
        SMSClient client = new SMSClient(masterSecret, appKey);

        SendSmsResp resp = new SendSmsResp();
        SMSPayload payload = SMSPayload.newBuilder()
                .setMobileNumber(phoneNumber)
                .setTempId(1)
                .build();
        TUserinfo userinfo = wxService.selectUserByPhoneNumber(phoneNumber);
        try {
            if (!StringUtils.isEmpty(userinfo)) {
                resp.setRetCode(2);
                resp.setRetDesc("该用户已经注册过了");
                return resp;
            }
            SendSMSResult smsResult = client.sendSMSCode(payload);
            if (StringUtils.isEmpty(smsResult)) {
                resp.setRetCode(1);
                resp.setRetDesc("验证码发送失败");
                return resp;
            }

            String originalContent = smsResult.getOriginalContent();
            JSONObject jb = JSONObject.parseObject(originalContent);
            String msgId = jb.getString("msg_id");

            logger.info("=========msgId========" + smsResult.toString());
            resp.setRetCode(0);
            resp.setRetDesc("验证码发送成功");
            resp.setSmsId(msgId);
            return resp;
        } catch (APIConnectionException e) {
            logger.error("Connection error. Should retry later. ", e);
        } catch (APIRequestException e) {
            logger.error("Error response from JPush server. Should review and fix it. ", e);
            logger.info("HTTP Status: " + e.getStatus());
            logger.info("Error Message: " + e.getMessage());
        }
        resp.setRetCode(1);
        resp.setRetDesc("验证码发送失败");
        return resp;
    }

    /**
     * 验证码校验接口
     *
     * @param verifyCode
     * @param msgId
     * @param openid
     * @param phoneNumber
     * @return {
     * "retCode": 0,
     * "retDesc": "验证码校验成功"
     * }
     */
    @RequestMapping(value = "/checkSmsCode.do")
    @ResponseBody
    public CheckSmsCodeResp checkSmsCode(String verifyCode,
                                         String msgId,
                                         String openid,
                                         String phoneNumber,
          /**/                           String recommenderId) {
        Integer recommenderid = 0;
        String sonId = "";
        String sonIds = "";

        if (!StringUtils.isEmpty(recommenderId)) {
            //26
            int userId = Integer.parseInt(recommenderId);
            TUserinfo tUserinfo = tUserinfoService.selectOpenidByUserId(userId);
            //-0-21
            sonId = tUserinfo.getSonId();
            sonIds = sonId + "-" + userId;
        }

        if (!StringUtils.isEmpty(recommenderId)) {
            /**/
            recommenderid = Integer.parseInt(recommenderId);
        }
        logger.info("=========openid========" + openid);

        CheckSmsCodeResp checkSmsCode = new CheckSmsCodeResp();
        TUserinfo userinfo = wxService.selectUserByPhoneNumber(phoneNumber);

        TUserinfo userinfoOp = wxService.selectUserByOpenId(openid);
        logger.info("==========userinfoOp===========" + userinfoOp.toString());
        if (!StringUtils.isEmpty(userinfo)) {
            checkSmsCode.setRetCode(2);
            checkSmsCode.setRetDesc("该用户已经注册了");
            return checkSmsCode;
        }
        try {
            SMSClient client = new SMSClient(masterSecret, appKey);
            ValidSMSResult res = client.sendValidSMSCode(msgId, verifyCode);
            if (StringUtils.isEmpty(res)) {
                checkSmsCode.setRetCode(1);
                checkSmsCode.setRetDesc("你输入的验证码有误");
                return checkSmsCode;
            }

            //验证成功后把号码更新到数据库表中
            //TODO
            /**/
            int i = wxService.updateByPrimaryKey(userinfoOp.getId(), phoneNumber, recommenderid, sonIds);
            logger.info("==========res.getIsValid()===========" + res.toString());
            checkSmsCode.setRetCode(0);
            checkSmsCode.setRetDesc("验证码校验成功");
            return checkSmsCode;
        } catch (APIConnectionException e) {
            logger.error("Connection error. Should retry later. ", e);
        } catch (APIRequestException e) {
            logger.error("Error response from JPush server. Should review and fix it. ", e);
            logger.info("HTTP Status: " + e.getStatus());
            logger.info("Error Message: " + e.getMessage());
        }

        checkSmsCode.setRetCode(1);
        checkSmsCode.setRetDesc("你输入的验证码有误");
        return checkSmsCode;
    }


    /**
     * 通过code获得openid
     *
     * @param code
     * @return
     */
    @RequestMapping(value = "/wxUserDetail")
    @ResponseBody
    public TUserinfo wxUserDetail(String code) {
        String url = "https://api.weixin.qq.com/sns/oauth2/access_token?" +
                "appid=" + XBMSConstant.XBMS_WX_APPID +
                "&secret=" + XBMSConstant.XBMS_WX_SECRET +
                "&code=" + code +
                "&grant_type=authorization_code";
        String postRequest = HttpUtil.getHttps(url);
        logger.info("=========postRequest=========" + postRequest);

        JSONObject object = JSONObject.parseObject(postRequest);
        String openid = object.getString("openid");
        return wxService.selectUserByOpenId(openid);
    }


    @RequestMapping(value = "/wxUserDetailByOpenid")
    @ResponseBody
    public TUserinfo getUserinfo(String openid) {
        return wxService.selectUserByOpenId(openid);
    }


    /**
     * 添加好友
     *
     * @param openId
     * @param phone
     * @param friendName
     * @return
     */
    @RequestMapping(value = "/friendAdd.do")
    @ResponseBody
    public CheckSmsCodeResp friendAdd(String openId, String phone, String friendName) {
        CheckSmsCodeResp resp = new CheckSmsCodeResp();
        String regex = "0?(13|14|15|16|17|18|19)[0-9]{9}";
        if (!phone.matches(regex)) {
            resp.setRetCode(2);
            resp.setRetDesc("您输入的号码格式有误");
            return resp;
        }

        TBook book = new TBook();
        book.setOpenId(openId);
        book.setPhone(phone);
        book.setFriendName(friendName);
        int i = tBookService.insert(book);

        if (i < 1) {
            resp.setRetCode(1);
            resp.setRetDesc("添加好友通讯录失败");
            return resp;
        }
        resp.setRetCode(0);
        resp.setRetDesc("添加好友通讯录成功");
        return resp;
    }


    /**
     * 删除好友
     *
     * @param openId
     * @param phone
     * @return
     */
    @RequestMapping(value = "/friendDel.do")
    @ResponseBody
    public CheckSmsCodeResp friendDel(String openId, String phone) {
        CheckSmsCodeResp resp = new CheckSmsCodeResp();
        int i = tBookService.delFriendByOpenIdAndPhone(openId, phone);

        if (i < 1) {
            resp.setRetCode(1);
            resp.setRetDesc("删除好友通讯录失败");
            return resp;
        }
        resp.setRetCode(0);
        resp.setRetDesc("删除好友通讯录成功");
        return resp;
    }


    /**
     * 修改好友
     *
     * @param openId
     * @param phone
     * @param friendName
     * @return
     */
    @RequestMapping(value = "/friendUpd.do")
    @ResponseBody
    public CheckSmsCodeResp friendUpd(@RequestParam(value = "openId") String openId, String phone,
                                      @RequestParam(value = "friendName", required = false) String friendName) {
        CheckSmsCodeResp resp = new CheckSmsCodeResp();

        logger.info("/friendUpd.do openId={},phone={},friendName={}", openId, phone, friendName);

        if (friendName == null || "".equals(friendName)) {
            friendName = phone;
            logger.info("friendName is null or blank, set value=" + phone);
        }
        String regex = "0?(13|14|15|16|17|18|19)[0-9]{9}";
        if (!phone.matches(regex)) {
            resp.setRetCode(2);
            resp.setRetDesc("您输入的号码格式有误");
            return resp;
        }


        int i = tBookService.updateFriendBookByPhone(phone, openId, friendName);

        if (i < 1) {
            resp.setRetCode(1);
            resp.setRetDesc("修改好友通讯录失败");
            return resp;
        }
        resp.setRetCode(0);
        resp.setRetDesc("修改好友通讯录成功");
        return resp;
    }


    /**
     * 查询好友详情
     *
     * @param openId
     * @param phone
     * @return
     */
    @RequestMapping(value = "/friendQry.do")
    @ResponseBody
    public QryFriendResp friendQry(String openId, @RequestParam(required = false) String phone) {
        QryFriendResp resp = new QryFriendResp();

        logger.info("friendQry.do openId={},phone={}", openId, phone);
        List<TBook> bookList = tBookService.qryFriendDetail(openId, phone);
        List<QryFriendRespDto> qryFriendRespDtos = new ArrayList<>();
        resp.setRetData(qryFriendRespDtos);

        if (CollectionUtils.isEmpty(bookList)) {
            resp.setRetCode(2);
            resp.setRetDesc("没有此好友记录");
            return resp;
        }

        for (TBook book : bookList) {
//            //每次都new一个对象
            QryFriendRespDto friendRespDto = new QryFriendRespDto();
//
//            TSet tSet = tSetService.selectBusinessVoiceByUserIdAndValue(tUserinfo.getId(), phone);
//            Integer voiceId = tSet.getVoiceId();
//            TVoice tVoice = tVoiceService.selectByVoiceId(voiceId);
//            Integer businessId = tVoice.getBusinessId();
//            TContent tContent = tContentService.selectContentByBusinessId(businessId);
//
//            if (StringUtils.isEmpty(tContent)) {
//                resp.setRetCode(1);
//                resp.setRetDesc("此智库不存在");
//                return resp;
//            }

            friendRespDto.setPhone(book.getPhone());
            friendRespDto.setFriendName(book.getFriendName());

            qryFriendRespDtos.add(friendRespDto);
        }

        resp.setRetCode(0);
        resp.setRetDesc("查询好友通讯录成功");

        return resp;
    }


    /**
     * 添加群组
     *
     * @param openId
     * @param groupName
     * @param groupMemberPhones
     * @return
     */
    @RequestMapping(value = "/groupAdd.do")
    @ResponseBody
    public CheckSmsCodeResp groupAdd(String openId, String groupName,
                                     @RequestParam(value = "groupMemberPhones", required = false) String groupMemberPhones) {
        CheckSmsCodeResp resp = new CheckSmsCodeResp();

        List<TGroup> groupList = tGoupService.selectGroupNameByOpenId(openId);
        for (TGroup group : groupList) {
            if (group.getGroupName().equals(groupName)) {
                resp.setRetCode(3);
                resp.setRetDesc("已存在相同群名称");
                return resp;
            }
        }
        String regex = "0?(13|14|15|16|17|18|19)[0-9]{9}";
        String[] split = groupMemberPhones.split(",");
        for (String s : split) {
            if (StringUtils.isEmpty(s)) {
                continue;
            }
            if (!s.matches(regex)) {
                resp.setRetCode(2);
                resp.setRetDesc("您输入的号码格式有误");
                return resp;
            }
        }
        TGroup group = new TGroup();
        group.setOpenId(openId);
        group.setGroupName(groupName);
        group.setGroupMemberphones(groupMemberPhones);

        int i = tGoupService.insert(group);
        if (i < 1) {
            resp.setRetCode(1);
            resp.setRetDesc("添加群组失败");
            return resp;
        }
        resp.setRetCode(0);
        resp.setRetDesc("添加群组成功");
        return resp;
    }


    /**
     * 删除群组
     *
     * @param openId
     * @param groupName
     * @return
     */
    @RequestMapping(value = "/groupDel.do")
    @ResponseBody
    public CheckSmsCodeResp groupDel(String openId, String groupName) {
        CheckSmsCodeResp resp = new CheckSmsCodeResp();
        int i = tGoupService.delGroupByOpenIdAndGroupName(openId, groupName);

        if (i < 1) {
            resp.setRetCode(1);
            resp.setRetDesc("删除群组失败");
            return resp;
        }
        resp.setRetCode(0);
        resp.setRetDesc("删除群组成功");
        return resp;
    }


    /**
     * 修改群组
     *
     * @param id
     * @param openId
     * @param groupName
     * @param groupMemberPhones
     * @return
     */
    @RequestMapping(value = "/groupUpd.do")
    @ResponseBody
    public CheckSmsCodeResp groupUpd(int id, String openId,
                                     @RequestParam(value = "groupName", required = false) String groupName,
                                     @RequestParam(value = "groupMemberPhones", required = false) String groupMemberPhones) {
        CheckSmsCodeResp resp = new CheckSmsCodeResp();
        String regex = "0?(13|14|15|16|17|18|19)[0-9]{9}";
        if (!StringUtils.isEmpty(groupMemberPhones)) {
            String[] split = groupMemberPhones.split(",");
            for (String s : split) {
                if (!s.matches(regex)) {
                    resp.setRetCode(2);
                    resp.setRetDesc("您输入的号码格式有误");
                    return resp;
                }
            }
        }

        int i = tGoupService.updateGroup(id, openId, groupName, groupMemberPhones);

        if (i < 1) {
            resp.setRetCode(1);
            resp.setRetDesc("修改群组失败");
            return resp;
        }
        resp.setRetCode(0);
        resp.setRetDesc("修改群组成功");
        return resp;
    }


    /**
     * 查询群组
     *
     * @param openId
     * @param groupName
     * @return
     */
    @RequestMapping(value = "/groupQry.do")
    @ResponseBody
    public QryGroupResp groupQry(String openId,
                                 @RequestParam(value = "groupName", required = false) String groupName) {
        QryGroupResp resp = new QryGroupResp();

        logger.info("/groupQry.do openId={},groupName={}", openId, groupName);
        List<GroupDto> groupDtoList = new ArrayList<>();
        List<TBook> bookList = new ArrayList<>();
        List<TGroup> groupList = tGoupService.selectGroupDetailByOpenidAndName(openId, groupName);
        if (CollectionUtils.isEmpty(groupList)) {
            resp.setRetCode(1);
            resp.setRetDesc("没有此群组记录");
            return resp;
        }
        for (TGroup group : groupList) {

            GroupDto groupDto = new GroupDto();
            String[] phoneStr = group.getGroupMemberphones().split(",");
            for (String phone : phoneStr) {
                TBook tBook = new TBook();
                TBook book = tBookService.qryFriendByOpenidAndPhone(openId, phone);
                if (book == null) {
                    continue;
                }
                tBook.setOpenId(openId);
                tBook.setPhone(book.getPhone());
                tBook.setFriendName(book.getFriendName());

                bookList.add(tBook);
            }

            groupDto.setGroupMemberPhones(bookList);
            groupDto.setId(group.getId());
            groupDto.setGroupName(group.getGroupName());
            groupDtoList.add(groupDto);
        }

        resp.setRetCode(0);
        resp.setRetDesc("查询群组成功");
        resp.setRetData(groupDtoList);
        return resp;
    }


    /**
     * 话术商城列表
     *
     * @param cpName
     * @param businessDesc
     * @return
     */
    @RequestMapping(value = "/contentQry.do")
    @ResponseBody
    public QryContentResp businessQry(@RequestParam(value = "cpName", required = false) String cpName,
                                      @RequestParam(value = "businessDesc", required = false) String businessDesc) {
        QryContentResp resp = new QryContentResp();
        List<QryContentDto> contentDtos = new ArrayList<>();
        List<TContent> contentList = tContentService.selectContentList(cpName, businessDesc);
        if (StringUtils.isEmpty(contentList)) {
            resp.setRetCode(1);
            resp.setRetDesc("商城为空");
            return resp;
        }

        for (TContent tContent : contentList) {
            QryContentDto contentDto = new QryContentDto();
            //话术
            contentDto.setId(tContent.getId());
            contentDto.setBusinessId(tContent.getBusinessId());
            contentDto.setBusinessImage(tContent.getBusinessImage());
            contentDto.setBusinessName(tContent.getBusinessName());
            contentDto.setBuyTimes(tContent.getBuyTimes());
            contentDto.setClickTimes(tContent.getClickTimes());
            contentDto.setCpId(tContent.getCpId());
            contentDto.setCpImage(tContent.getCpImage());
            contentDto.setCpName(tContent.getCpName());
            contentDto.setCreateTime(tContent.getCreateTime());
            contentDto.setBusinessDesc(tContent.getBusinessDesc());

            /*List<TVoice> tVoiceList = tVoiceService.selectByBusinessId(tContent.getBusinessId());
            if (CollectionUtils.isEmpty(tVoiceList)) {
                continue;
            }
            for (TVoice tVoice : tVoiceList) {
//                tUserVoiceService.selectVoiceStatusById(tVoice.getId());
                //声音
                contentDto.setType(tVoice.getType());
                contentDto.setVoiceId(tVoice.getId());
                contentDto.setVoiceImage(tVoice.getVoiceImage());
                contentDto.setVoiceName(tVoice.getVoiceName());
                contentDto.setVoicePath(tVoice.getVoicePath());
                contentDto.setVpId(tVoice.getVpId());
                contentDto.setVpName(tVoice.getVpName());

                contentDtos.add(contentDto);
            }*/
            contentDtos.add(contentDto);
        }

        resp.setRetCode(0);
        resp.setRetDesc("查询商城成功");
        resp.setRetData(contentDtos);
        return resp;
    }


    /**
     * 购买话术
     *
     * @param userId
     * @param voiceId
     * @return
     */
    @RequestMapping(value = "/userBuyVoice.do")
    @ResponseBody
    public CheckSmsCodeResp userBuyVoice(Integer userId, Integer voiceId) {
        CheckSmsCodeResp resp = new CheckSmsCodeResp();
        TUserVoice userVoice = new TUserVoice();
        userVoice.setUserId(userId);
        userVoice.setVoiceId(voiceId);

        int i = tUserVoiceService.insertVoiceToUserCenter(userVoice);
        if (i < 1) {
            resp.setRetCode(1);
            resp.setRetDesc("订购声音失败");
            return resp;
        }
        resp.setRetCode(0);
        resp.setRetDesc("订购声音成功");
        return resp;
    }


    /**
     * 查询话术设置页面
     *
     * @param userId
     * @return
     */
    @RequestMapping(value = "/speechcraftSetQry.do")
    @ResponseBody
    public SpeechcraftSetResp speechcraftSetQry(Integer userId) {

        SpeechcraftSetResp resp = new SpeechcraftSetResp();

        //群组
        LabelVo groupVo = new LabelVo();
        groupVo.setLabelVoList(new ArrayList<>());
        groupVo.setTitleId("88888888");
        groupVo.setTitleName("群组设置");
        groupVo.setImageUrl("http://thirdwx.qlogo.cn/mmopen/mDRsDt5SWPQTQL6ticKvnWDYTk5Xc54beXtxptRWV0uZRanFGLJPV2MO2HuvNea5AV7SicGibdLd6QiasMxIESKzkEEQIN4WeQ19/132");
        groupVo.setShowStyle(1);

        resp.getRetData().add(groupVo);

        TUserinfo tUserinfo = tUserinfoService.selectOpenidByUserId(userId);
        List<TGroup> groupList = tGoupService.selectGroupNameByOpenId(tUserinfo.getOpenid());

        for (TGroup group : groupList) {
            LabelVo LabelVo2 = new LabelVo();
            LabelVo2.setTitleId("" + group.getId());
            LabelVo2.setTitleName(group.getGroupName());
            // LabelVo2.setImageUrl();
            groupVo.getLabelVoList().add(LabelVo2);
        }


        //好友
        LabelVo bookVo = new LabelVo();
        bookVo.setLabelVoList(new ArrayList<>());
        bookVo.setTitleId("99999999");
        bookVo.setTitleName("通讯录设置");
        bookVo.setImageUrl("http://thirdwx.qlogo.cn/mmopen/mDRsDt5SWPQTQL6ticKvnWDYTk5Xc54beXtxptRWV0uZRanFGLJPV2MO2HuvNea5AV7SicGibdLd6QiasMxIESKzkEEQIN4WeQ19/132");
        bookVo.setShowStyle(2);

        resp.getRetData().add(bookVo);


        List<TBook> tBookList = tBookService.selectFriendByOpenId(tUserinfo.getOpenid());
        for (TBook tBook : tBookList) {
            LabelVo LabelVo2 = new LabelVo();
            LabelVo2.setTitleId(tBook.getPhone());
            LabelVo2.setTitleName(tBook.getFriendName());

            bookVo.getLabelVoList().add(LabelVo2);
        }


        //默认
        List<TLabel> allList = tLabelService.selectAllData();
        LabelVo labelVo1 = null;

        for (TLabel tLabel : allList) {

            if ("1".equals(tLabel.getLevel())) {
                labelVo1 = new LabelVo();
                labelVo1.setLabelVoList(new ArrayList<>());
                labelVo1.setTitleId("" + tLabel.getId());
                labelVo1.setTitleName(tLabel.getName());
                labelVo1.setImageUrl(tLabel.getImageurl());
                labelVo1.setShowStyle(5);

                resp.getRetData().add(labelVo1);
            }
            if ("2".equals(tLabel.getLevel())) {
                LabelVo labelVo2 = new LabelVo();
                labelVo2.setTitleId("" + tLabel.getId());
                labelVo2.setTitleName(tLabel.getName());
                labelVo2.setImageUrl(tLabel.getImageurl());
                labelVo2.setShowStyle(5);

                labelVo1.getLabelVoList().add(labelVo2);
            }
        }


        //声音


        //话术
        List<Integer> businessIds = new ArrayList<>();
        Set<Integer> integerSet = new HashSet<>();
        LabelVo businessVo = new LabelVo();
        businessVo.setLabelVoList(new ArrayList<>());
        businessVo.setTitleId("77777777");
        businessVo.setTitleName("话术设置");
        businessVo.setImageUrl("http://thirdwx.qlogo.cn/mmopen/mDRsDt5SWPQTQL6ticKvnWDYTk5Xc54beXtxptRWV0uZRanFGLJPV2MO2HuvNea5AV7SicGibdLd6QiasMxIESKzkEEQIN4WeQ19/132");
        businessVo.setShowStyle(4);

        resp.getRetData().add(businessVo);

        List<TUserVoice> tUserVoiceList = tUserVoiceService.selectVoiceIdByUserId(userId);
        logger.info("==========tUserVoiceList={}===========", tUserVoiceList.size());

        for (TUserVoice userVoice : tUserVoiceList) {

            TVoice tVoice = tVoiceService.selectByVoiceId(userVoice.getVoiceId());
            Integer id = tVoice.getBusinessId();
            businessIds.add(id);
            /*TVoice tVoice = tVoiceService.selectByVoiceId(userVoice.getVoiceId());
            Integer businessId = tVoice.getBusinessId();//1,1,1
            LabelVo labelVo = new LabelVo();
            TContent tContent = tContentService.selectContentByBusinessId(businessId);
            labelVo.setTitleId("" + tContent.getBusinessId());
            labelVo.setTitleName(tContent.getBusinessName());
            labelVo.setImageUrl(tContent.getCpImage());

            businessVo.getLabelVoList().add(labelVo);*/

        }
        logger.info("======原集合====businessIds={}======{}=====", businessIds.size(), businessIds.toString());
        integerSet.addAll(businessIds);
        logger.info("======去重后====businessIds={}======{}=====", integerSet.size(), integerSet.toString());
        for (Integer businessId : integerSet) {
            LabelVo labelVo = new LabelVo();
            TContent tContent = tContentService.selectContentByBusinessId(businessId);
            labelVo.setTitleId("" + tContent.getBusinessId());
            labelVo.setTitleName(tContent.getBusinessName());
            labelVo.setImageUrl(tContent.getCpImage());

            businessVo.getLabelVoList().add(labelVo);
        }

        resp.setRetCode(0);
        resp.setRetDesc("查询话术设置页面成功");

        return resp;
    }


    /**
     * 查询商城页面
     *
     * @return
     */
    @RequestMapping(value = "/mallQry.do")
    @ResponseBody
    public MallResp mallQry(@RequestParam(value = "userId", required = false) Integer userId) {

        MallResp resp = new MallResp();
        List<TMall> defaulList = tMallService.selectAllDataDefaul();

        //默认
        List<TMall> tMallList = tMallService.selectAllData(userId);
        logger.info("========tMallList={}====tMallList.size()={}=====", tMallList, tMallList.size());
        MallVo mallVo1 = null;

        for (TMall tMall : tMallList) {
//            logger.info("=========66666==========");
            if (tMall.getLevel() == 1) {
                mallVo1 = new MallVo();
                mallVo1.setLabelVoList(new ArrayList<>());
                mallVo1.setTitleId("" + tMall.getId());
                mallVo1.setTitleName(tMall.getName());
                mallVo1.setImageUrl(tMall.getImageUrl());
                mallVo1.setShowStyle(5);
//                logger.info("=========mallVo1={}==========", mallVo1);

                resp.getRetData().add(mallVo1);
            }
            if (tMall.getLevel() == 2) {
                MallVo mallVo2 = new MallVo();
                mallVo2.setTitleId("" + tMall.getId());
                mallVo2.setTitleName(tMall.getName());
                mallVo2.setImageUrl(tMall.getImageUrl());
                mallVo2.setShowStyle(5);
//                logger.info("=========mallVo2={}==========", mallVo2);

                mallVo1.getLabelVoList().add(mallVo2);

            }

        }
        List<TMall> del = new ArrayList<>();
        for (TMall tMall : defaulList) {
            for (TMall in : tMallList) {

                if (in.getId().equals(tMall.getId()) && tMall.getLevel() == 2) {
                    del.add(tMall);
//                    logger.info("=====in.getId()={}======", in.getId());
//                    break;
                }
            }
        }
        defaulList.removeAll(del);

        for (TMall tMall : defaulList) {
//            logger.info("=========66666==========");
            if (tMall.getLevel() == 1) {
                mallVo1 = new MallVo();
                mallVo1.setLabelVoList(new ArrayList<>());
                mallVo1.setTitleId("" + tMall.getId());
                mallVo1.setTitleName(tMall.getName());
                mallVo1.setImageUrl(tMall.getImageUrl());
                mallVo1.setShowStyle(5);
//                logger.info("=========mallVo1={}==========", mallVo1);

                resp.getRetDataNo().add(mallVo1);
            }
            if (tMall.getLevel() == 2) {
                MallVo mallVo2 = new MallVo();
                mallVo2.setTitleId("" + tMall.getId());
                mallVo2.setTitleName(tMall.getName());
                mallVo2.setImageUrl(tMall.getImageUrl());
                mallVo2.setShowStyle(5);
//                logger.info("=========mallVo2={}==========", mallVo2);
                mallVo1.getLabelVoList().add(mallVo2);
            }
        }

        logger.info("=======defaulList.size={}=========", defaulList.size());
        logger.info("=======del.size={}=========", del.size());

        resp.setRetCode(0);
        resp.setRetDesc("查询商城页面成功");

        return resp;
    }


    /**
     * 勾选我感兴趣的标签
     *
     * @param id
     * @param userId
     * @return
     */
    @RequestMapping(value = "/insertLabel.do")
    @ResponseBody
    public CheckSmsCodeResp insertlabel(Integer id, Integer userId) {
        CheckSmsCodeResp resp = new CheckSmsCodeResp();
        TMall tMall1 = tMallService.selectByIdAndDefault(id);
        Integer fatherId = tMall1.getFatherId();
        TMall tMall2 = tMallService.selectByIdAndDefault(fatherId);

        TMall tMall = new TMall();
        tMall.setId(id);
        tMall.setUserId(userId);
        tMall.setName(tMall1.getName());
        tMall.setLevel(tMall1.getLevel());
        tMall.setFatherId(tMall1.getFatherId());
        tMall.setImageUrl(tMall1.getImageUrl());

        TMall tMal2 = new TMall();
        tMal2.setId(fatherId);
        tMal2.setUserId(userId);
        tMal2.setName(tMall2.getName());
        tMal2.setLevel(tMall2.getLevel());
        tMal2.setFatherId(tMall2.getFatherId());
        tMal2.setImageUrl(tMall2.getImageUrl());

        TMall tMall3 = tMallService.selectByIdAndUserid(id, userId);
        if (!StringUtils.isEmpty(tMall3)) {
            resp.setRetCode(1);
            resp.setRetDesc("已勾选，不能重复添加");
            return resp;
        }
        int i = tMallService.insertlabel(tMall);
        if (i < 1) {
            resp.setRetCode(1);
            resp.setRetDesc("添加失败");
            return resp;
        }

        TMall tMall4 = tMallService.selectByIdAndUserid(fatherId, userId);
        if (StringUtils.isEmpty(tMall4)) {
            int j = tMallService.insertlabel(tMal2);
        }
        resp.setRetCode(0);
        resp.setRetDesc("添加成功");
        return resp;
    }


    /**
     * 删除我选中的标签
     *
     * @param id
     * @param userId
     * @return
     */
    @RequestMapping(value = "/deleteLabel.do")
    @ResponseBody
    public CheckSmsCodeResp deleteLabel(Integer id, Integer userId) {
        CheckSmsCodeResp resp = new CheckSmsCodeResp();

        int i = tMallService.deleteLabel(id, userId);
        if (i < 1) {
            resp.setRetCode(1);
            resp.setRetDesc("删除失败");
            return resp;
        }
        resp.setRetCode(0);
        resp.setRetDesc("删除成功");
        return resp;
    }


    @RequestMapping(value = "/myVoiceQry.do")
    @ResponseBody
    public VoiceDtoResp myVoiceQry(Integer userId) {

        List<VoiceDto> voiceDtoList = new ArrayList<>();
        VoiceDtoResp resp = new VoiceDtoResp();

        List<TVoice> tVoiceList = tVoiceService.selectByUserId(userId);
        logger.info("=========tVoiceList={}===========", tVoiceList.size());
        if (!CollectionUtils.isEmpty(tVoiceList)) {
            for (TVoice tVoice : tVoiceList) {
                VoiceDto voiceDto = new VoiceDto();
                voiceDto.setVoiceId(tVoice.getId());
                voiceDto.setVoiceName(tVoice.getVoiceName());
                voiceDto.setVoiceImage(tVoice.getVoiceImage());
                voiceDtoList.add(voiceDto);
            }

            resp.setRetCode(0);
            resp.setRetDesc("ok");
            resp.setRetData(voiceDtoList);
            return resp;
        }
        return null;
    }


    /**
     * 查看我滴设置
     *
     * @param userId
     * @return
     */
    @RequestMapping(value = "/speechcraftSetInfo.do")
    @ResponseBody
    public SetInfoDtoResp speechcraftSetInfo(Integer userId) {
        SetInfoDtoResp resp = new SetInfoDtoResp();

        List<Integer> integerList = new ArrayList<>();
        List<TSet> tSetList = tSetService.selectByUserId(userId);
        logger.info("======tSetList={}=======", tSetList.size());

        int operationId = 0;


        SetInfoDto setInfoDto = null;
        for (TSet set : tSetList) {
            if (operationId != set.getOperationId()) {
                operationId = set.getOperationId();
                setInfoDto = new SetInfoDto();
                resp.getRetDataList().add(setInfoDto);
                setInfoDto.setOperationId(operationId);
                setInfoDto.setVoiceName(set.getValue());
                setInfoDto.setCreateTime(set.getCreateTime());
                setInfoDto.setContentName(set.getFileName());
                setInfoDto.setUserId(userId);
            }
            if (set.getContent() != null && set.getContent().length() > 0) {
                setInfoDto.getLabelList().add(set.getContent());
            }


        }
        logger.info("======integerList={}=======", integerList);

        resp.setRetCode(0);
        resp.setRetDesc("OK");
        return resp;

    }


    /**
     * 查询单个话术下的多个声音
     *
     * @param businessId
     * @return
     */
    @RequestMapping(value = "/businessQry.do")
    @ResponseBody
    public List<TVoice> businessQry(Integer businessId, Integer userId) {
        List<TVoice> voices = new ArrayList<>();
        if (userId != null) {
            List<TVoice> tVoiceList = tVoiceService.selectByBusinessIdAndUserId(businessId, userId);
            for (TVoice voice : tVoiceList) {
                TVoice tVoice = new TVoice();
                tVoice.setId(voice.getId());
                tVoice.setVpName(voice.getVpName());
                tVoice.setVpId(voice.getVpId());
                tVoice.setVoicePath(voice.getVoicePath());
                tVoice.setVoiceName(voice.getVoiceName());
                tVoice.setVoiceImage(voice.getVoiceImage());
                tVoice.setType(voice.getType());
                tVoice.setCreateTime(voice.getCreateTime());
                tVoice.setBusinessId(voice.getBusinessId());
                voices.add(tVoice);
            }
            return voices;
        }

        List<TVoice> tVoiceList = tVoiceService.selectByBusinessId(businessId);
        for (TVoice voice : tVoiceList) {
            TVoice tVoice = new TVoice();
            tVoice.setId(voice.getId());
            tVoice.setVpName(voice.getVpName());
            tVoice.setVpId(voice.getVpId());
            tVoice.setVoicePath(voice.getVoicePath());
            tVoice.setVoiceName(voice.getVoiceName());
            tVoice.setVoiceImage(voice.getVoiceImage());
            tVoice.setType(voice.getType());
            tVoice.setCreateTime(voice.getCreateTime());
            tVoice.setBusinessId(voice.getBusinessId());
            voices.add(tVoice);
        }
        return voices;
    }


    /**
     * 设置接口
     *
     * @param request
     * @param labelVo
     * @return [
     * {
     * "titleId": "88888888",
     * "titleName": "群组设置",
     * "imageUrl": "http://thirdwx.qlogo.cn/mmopen/mDRsDt5SWPQTQL6ticKvnWDYTk5Xc54beXtxptRWV0uZRanFGLJPV2MO2HuvNea5AV7SicGibdLd6QiasMxIESKzkEEQIN4WeQ19/132",
     * "showStyle": 1,
     * "labelVoList": [
     * {
     * "titleId": "1",
     * "titleName": "45645644"
     * },
     * {
     * "titleId": "4",
     * "titleName": "leigroup"
     * },
     * {
     * "titleId": "5",
     * "titleName": "天霸动霸tua"
     * },
     * {
     * "titleId": "9",
     * "titleName": "555"
     * }
     * ]
     * },
     * {
     * "titleId": "99999999",
     * "titleName": "通讯录设置",
     * "imageUrl": "http://thirdwx.qlogo.cn/mmopen/mDRsDt5SWPQTQL6ticKvnWDYTk5Xc54beXtxptRWV0uZRanFGLJPV2MO2HuvNea5AV7SicGibdLd6QiasMxIESKzkEEQIN4WeQ19/132",
     * "showStyle": 2,
     * "labelVoList": [
     * {
     * "titleId": "13620203403",
     * "titleName": "dujuan"
     * },
     * {
     * "titleId": "15562234654",
     * "titleName": "小慧"
     * },
     * {
     * "titleId": "15625252525",
     * "titleName": "芊芊"
     * },
     * {
     * "titleId": "17687663373",
     * "titleName": "龙哥"
     * },
     * {
     * "titleId": "18750031121",
     * "titleName": "小滢子1"
     * }
     * ]
     * },
     */
    @RequestMapping(value = "/set.do")
    @ResponseBody
    public CheckSmsCodeResp set(HttpServletRequest request, @RequestBody LabelVo[] labelVo) {
        CheckSmsCodeResp resp = new CheckSmsCodeResp();
        Integer voiceId = 0;
        String userId = request.getParameter("userId");
        String content = request.getParameter("content");
        String voice = request.getParameter("voice");

        if (StringUtils.isEmpty(userId)) {
            resp.setRetCode(1);
            resp.setRetDesc("userId为null");
            return resp;
        }
        Integer temp = seq++;

        String fileName = "";
        try {
            if (content != null && content.trim().length() > 0) {
                fileName = MD5.MD5_32bit(content) + ".wav";
                new Thread() {
                    @Override
                    public void run() {
                        customPrologue(content, voice);
                    }
                }.start();
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        logger.info("=======userId===={}==", userId);
        logger.info("========labelVo===={}====", labelVo.toString());

        List<TSet> tSetList = new ArrayList<>();
        for (LabelVo vo : labelVo) {
            //先获取声音id
            if ("77777777".equals(vo.getTitleId())) {
                List<LabelVo> labelVoList = vo.getLabelVoList();
                LabelVo labelVo1 = labelVoList.get(0);
                voiceId = Integer.parseInt(labelVo1.getTitleId());
                logger.info("=========voiceId=={}=======", voiceId);
            }

            //群组
            if ("88888888".equals(vo.getTitleId())) {
                for (LabelVo labelVo1 : vo.getLabelVoList()) {
                    String groupId = labelVo1.getTitleId();
                    TSet tSet = new TSet();
                    tSet.setType("2");
                    tSet.setValue(groupId);
//                    tSet.setVoiceId(voiceId);
                    tSet.setUserId(Integer.parseInt(userId));
                    tSet.setContent(content);
                    tSet.setFileName(fileName);
                    tSet.setFileName(fileName);
                    tSet.setOperationId(temp);
                    tSetList.add(tSet);
                }
            }

            //通讯录
            if ("99999999".equals(vo.getTitleId())) {
                for (LabelVo labelVo1 : vo.getLabelVoList()) {
                    String phone = labelVo1.getTitleId();
                    TSet tSet = new TSet();
                    tSet.setType("1");
                    tSet.setValue(phone);
//                    tSet.setVoiceId(voiceId);
                    tSet.setUserId(Integer.parseInt(userId));
                    tSet.setContent(content);
                    tSet.setFileName(fileName);
                    tSet.setOperationId(temp);
                    tSetList.add(tSet);
                }

            }

            //系统标签
            if ("系统标签".equals(vo.getTitleName())) {
                for (LabelVo labelVo1 : vo.getLabelVoList()) {
                    String id = labelVo1.getTitleId();
                    TSet tSet = new TSet();
                    tSet.setType("3");
                    tSet.setValue(id);
//                    tSet.setVoiceId(voiceId);
                    tSet.setUserId(Integer.parseInt(userId));
                    tSet.setContent(content);
                    tSet.setFileName(fileName);
                    tSet.setOperationId(temp);
                    tSetList.add(tSet);
                }

            }

            //情景模式
            if ("情景模式".equals(vo.getTitleName())) {
                for (LabelVo labelVo1 : vo.getLabelVoList()) {
                    String id = labelVo1.getTitleId();
                    TSet tSet = new TSet();
                    tSet.setType("4");
                    tSet.setValue(id);
//                    tSet.setVoiceId(voiceId);
                    tSet.setUserId(Integer.parseInt(userId));
                    tSet.setContent(content);
                    tSet.setFileName(fileName);
                    tSet.setOperationId(temp);
                    tSetList.add(tSet);
                }
            }
        }

        for (TSet tSet : tSetList) {
            tSet.setVoiceId(voiceId);
        }
        tSetService.addTSetList(tSetList);

        resp.setRetCode(0);
        resp.setRetDesc("设置成功");
        return resp;
    }


    /**
     * 自定义开场白
     *
     * @param content
     * @param voice
     * @return
     */
    @RequestMapping(value = "/customPrologue.do")
    @ResponseBody
    public Result<String> customPrologue(String content, String voice) {
        Result<String> stringResult = null;
        String fileName = null;
        String filePath = 2122 + "/BUSINESS-" + 134680578;
        String url = "https://ai.yousayido.net/recordManagement/";
        try {
            fileName = MD5.MD5_32bit(content + System.currentTimeMillis()) + ".wav";
            stringResult = voiceService.ttsVoice(filePath, fileName, content + voice, TtsVoiceNameEnum.PQ);
            long size;
            if (stringResult.isStatus()) {
                size = Long.valueOf(stringResult.getInfo());
            } else {
                size = 0L;
            }
            logger.info("小兵秘书自定义开场白调用tts合成服务返回结果:{}", JSONObject.toJSONString(stringResult));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        stringResult.setData(url + stringResult.getData());
        return stringResult;
    }


    /**
     * 查询我的状态（情景模式）
     *
     * @param id
     * @return
     */
    /*@RequestMapping(value = "/mySceneQry.do")
    @ResponseBody
    public ScenceAndInteresdResp mySceneQry(Integer id) {
        ScenceAndInteresdResp resp = new ScenceAndInteresdResp();
        TMall tMall = new TMall();
        List<TMall> tMallList = new ArrayList<>();

        TMall mall = tMallService.selectById(id);
        tMall.setId(mall.getId());
        tMall.setImageUrl(mall.getImageUrl());
        tMall.setFatherId(mall.getFatherId());
        tMall.setName(mall.getName());
        tMall.setLevel(mall.getLevel());
        tMallList.add(tMall);

        List<TMall> mallList = tMallService.selectByFatherId(id);
        for (TMall tMall1 : mallList) {
            TMall mall1 = new TMall();
            mall1.setId(tMall1.getId());
            mall1.setImageUrl(tMall1.getImageUrl());
            mall1.setFatherId(tMall1.getFatherId());
            mall1.setName(tMall1.getName());
            mall1.setLevel(tMall1.getLevel());
            tMallList.add(mall1);
        }

        resp.setRetCode(0);
        resp.setRetDesc("ok");
        resp.setRetData(tMallList);
        return resp;
    }
*/

    /**
     * 接听清单
     *
     * @param userId
     * @return
     */
    @RequestMapping(value = "/answerListQry.do")
    @ResponseBody
    public List<DialogDto> answerListQry(Integer userId) {
        List<DialogDto> dialogDtoList = new ArrayList<>();
        TUserinfo tUserinfo = tUserinfoService.selectOpenidByUserId(userId);
        if (StringUtils.isEmpty(tUserinfo)) {
            return null;
        }
        String phonenumber = tUserinfo.getPhonenumber();
        List<TDialog> tDialogList = tDialogService.selectByTaskId(phonenumber);
        if (CollectionUtils.isEmpty(tDialogList)) {
            return null;
        }
        for (TDialog tDialog : tDialogList) {
            TDialogDetailExt tDialogDetailExt = tDialogDetailExtProxy.selectByDialoginId(tDialog.getId());
            logger.info("=========tDialogDetailExt={}==========", tDialogDetailExt);
            String detailRecords = tDialogDetailExt.getDetailRecords();
            logger.info("=========detailRecords={}==========", detailRecords);
            JSONArray jsonArray = JSONArray.parseArray(detailRecords);
            logger.info("=========jsonArray={}==========", jsonArray);
            if (jsonArray.size() < 2) {
                continue;
            }
            Object o = jsonArray.get(1);
            JSONObject jsonObject = JSONObject.parseObject(o.toString());
            Object content_customer = jsonObject.get("content_customer");
            JSONArray array = JSONArray.parseArray(content_customer.toString());
            String simpleWord = (String) array.get(0);
            logger.info("=========simpleWord={}==========", simpleWord);

            DialogDto dialogDto = new DialogDto();
            dialogDto.setId(new Long(tDialog.getId()).intValue());
            dialogDto.setBeginDate(tDialog.getBeginDate());
            dialogDto.setCalledPhone(tDialog.getTaskId() + "");
            dialogDto.setCallerPhone(tDialog.getTelephone());
            dialogDto.setCreateDate(tDialog.getCreateDate());
            dialogDto.setEndDate(tDialog.getEndDate());
            dialogDto.setTotal_seconds(tDialog.getTotal_seconds());
            dialogDto.setSimpleWord(simpleWord);
            dialogDtoList.add(dialogDto);
        }
        return dialogDtoList;
    }


    /**
     * 添加撤销我的状态
     *
     * @param userId
     * @param id
     * @param isCheck
     * @return
     */
    @RequestMapping(value = "/myStatusChange.do")
    @ResponseBody
    public CheckSmsCodeResp myStatusChange(Integer userId, Integer id, boolean isCheck) {
        CheckSmsCodeResp resp = new CheckSmsCodeResp();
        TUserinfo tUserinfo = tUserinfoService.selectOpenidByUserId(userId);
        Integer status = tUserinfo.getStatus();
        if (id.equals(status)) {
            if (isCheck == true) {
                int i = tUserinfoService.updateMyStatus(userId, id);
                if (i < 1) {
                    resp.setRetCode(1);
                    resp.setRetDesc("添加我的状态失败");
                    return resp;
                }
                resp.setRetCode(0);
                resp.setRetDesc("添加我的状态成功");
                return resp;
            }

            int i = tUserinfoService.cancelMyStatus(userId, id);
            if (i < 1) {
                resp.setRetCode(1);
                resp.setRetDesc("撤销我的状态失败");
                return resp;
            }
            resp.setRetCode(0);
            resp.setRetDesc("撤销我的状态成功");
            return resp;
        }

        int i = tUserinfoService.cancelAndUpdateMyStatus(userId, id);
        if (i < 1) {
            resp.setRetCode(1);
            resp.setRetDesc("修改我的状态失败");
            return resp;
        }
        resp.setRetCode(0);
        resp.setRetDesc("修改我的状态成功");
        return resp;
    }


    /**
     * 切换我的声音
     *
     * @param userId
     * @param isCheck
     * @return
     */
    @RequestMapping(value = "/myVoiceChange.do")
    @ResponseBody
    public CheckSmsCodeResp myVoiceChange(Integer userId, Integer voiceId, boolean isCheck) {
        CheckSmsCodeResp resp = new CheckSmsCodeResp();
        if (isCheck == true) {
            int i = tUserinfoService.updateMyVoice(userId, voiceId);
            if (i < 1) {
                resp.setRetCode(1);
                resp.setRetDesc("添加我的声音失败");
                return resp;
            }
            resp.setRetCode(0);
            resp.setRetDesc("添加我的声音成功");
            return resp;
        }

        int i = tUserinfoService.cancelMyVoice(userId, voiceId);
        if (i < 1) {
            resp.setRetCode(1);
            resp.setRetDesc("修改我的声音失败");
            return resp;
        }
        resp.setRetCode(0);
        resp.setRetDesc("修改我的声音成功");
        return resp;
    }


    /**
     * 查看我的状态(所勾选的情景模式)
     *
     * @param userId
     * @return
     */
    @RequestMapping(value = "/myStatusQry.do")
    @ResponseBody
    public TMall myStatusQry(Integer userId) {
        TUserinfo tUserinfo = tUserinfoService.selectOpenidByUserId(userId);
        TMall tMall1 = new TMall();
        if (!StringUtils.isEmpty(tUserinfo)) {
            Integer status = tUserinfo.getStatus();
            if (!StringUtils.isEmpty(status)) {
                TMall tMall = tMallService.selectById(status, userId);
                logger.info("=========tMall={}=========", tMall);
                tMall1.setId(tMall.getId());
                tMall1.setFatherId(tMall.getFatherId());
                tMall1.setImageUrl(tMall.getImageUrl());
                tMall1.setLevel(tMall.getLevel());
                tMall1.setName(tMall.getName());
                tMall1.setUserId(tMall.getUserId());
                return tMall1;
            }
        }
        return new TMall(-1, "不好意思，你没有设置情景状态~~");
    }


    /**
     * 查看我的声音(所勾选的声音)
     *
     * @param userId
     * @return
     */
    @RequestMapping(value = "/myIscheckedVoiceQry.do")
    @ResponseBody
    public MyVoice myIscheckedVoiceQry(Integer userId) {
        TUserinfo tUserinfo = tUserinfoService.selectOpenidByUserId(userId);
        if (!StringUtils.isEmpty(tUserinfo)) {
            Integer voiceId = tUserinfo.getVoiceId();
            if (!StringUtils.isEmpty(voiceId)) {
                if (voiceId == 1) {
                    MyVoice voice = new MyVoice();
                    voice.setVoiceId(voiceId);
                    voice.setVoiceName("小凡");
                    voice.setVoiceImage("==========");
                    return voice;
                } else if (voiceId == 2) {
                    MyVoice voice = new MyVoice();
                    voice.setVoiceId(voiceId);
                    voice.setVoiceName("小芳");
                    voice.setVoiceImage("==========");
                    return voice;
                } else if (voiceId == 3) {
                    MyVoice voice = new MyVoice();
                    voice.setVoiceId(voiceId);
                    voice.setVoiceName("TTS男声");
                    voice.setVoiceImage("==========");
                    return voice;
                } else {
                    MyVoice voice = new MyVoice();
                    voice.setVoiceId(voiceId);
                    voice.setVoiceName("TTS女声");
                    voice.setVoiceImage("==========");
                    return voice;
                }
            }
        }
        return new MyVoice(-1, "不好意思，你没有设置声音~~");
    }


    /**
     * 来电推送
     * <p>
     * {
     * "data": {
     * "keyword3": {
     * "value": "18"
     * },
     * "keyword1": {
     * "value": "13613004847"
     * },
     * "keyword2": {
     * "value": "2019-10-11 18:17:21"
     * },
     * "remark": {
     * "value": "这是一条备注信息~"
     * },
     * "first": {
     * "value": "您好，小兵秘书为您接听到一条未接来电"
     * }
     * },
     * "template_id": "tfTD0HlAsHL4v40JBaGlSltl83tIMndWJNtKPpnw4kk",
     * "touser": "opNfewsLHoAkgEj9dpx6ej2zIt0M",
     * "url": "http://ai.yousayido.net/wxgzh/templates/calldetail.html?dialogId=1197808"
     * }
     * <p>
     * {
     * "errcode": 0,
     * "errmsg": "ok",
     * "msgid": 1053774636804210700
     * }
     *
     * @param userId
     */
    @RequestMapping(value = "/callPush.do")
    @ResponseBody
    public String callPush(Integer userId) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        TUserinfo tUserinfo = tUserinfoService.selectOpenidByUserId(userId);
        String openid = tUserinfo.getOpenid();

        List<TDialog> tDialogList = tDialogService.selectAnswerListByUserId(userId);
        logger.info("========tDialogList={}=======", tDialogList);
        for (TDialog tDialog : tDialogList) {
            String telephone = tDialog.getTelephone();
            Date beginDate = tDialog.getBeginDate();
            Integer total_seconds = tDialog.getTotal_seconds();
//            TemplateSendRequest templateSendReq = new TemplateSendRequest();
            TemplateSendRequestDto templateSendRequestDto = new TemplateSendRequestDto();

            String title = "您好，小兵秘书为您接听到一条未接来电";
            TemplateParamRequest templateParamRequest = new TemplateParamRequest();
            TemplateParamRequest templateParamRequest1 = new TemplateParamRequest();
            TemplateParamRequest templateParamRequest2 = new TemplateParamRequest();
            TemplateParamRequest templateParamRequest3 = new TemplateParamRequest();
            TemplateParamRequest templateParamRequest4 = new TemplateParamRequest();

            templateParamRequest.setValue(title);
            templateParamRequest1.setValue(telephone);
            templateParamRequest1.setColor("#1c4587");

            templateParamRequest2.setValue(format.format(beginDate));
            templateParamRequest2.setColor("#1c4587");

            templateParamRequest3.setValue(total_seconds + "秒");
            templateParamRequest3.setColor("#1c4587");

            templateParamRequest4.setValue("请点击下方查看详情~~");
            templateParamRequest4.setColor("#1c4587");

            Map<String, TemplateParamRequest> data = new HashMap<>();
            data.put("first", templateParamRequest);
            data.put("keyword1", templateParamRequest1);
            data.put("keyword2", templateParamRequest2);
            data.put("keyword3", templateParamRequest3);
            data.put("remark", templateParamRequest4);

            String url = String.format("http://ai.yousayido.net/wxgzh/templates/calldetail.html?dialogId=" + tDialog.getId());
            templateSendRequestDto.setTouser(openid);
            templateSendRequestDto.setTemplate_id(XBMSConstant.XBMS_WX_TEMPLATE_ID);
            templateSendRequestDto.setUrl(url);
            templateSendRequestDto.setData(data);

            String accessToken = "26_5A4rupUUQ5iSQ7xAFrUAbmspbv2mbaac7iqmZuTNI38O3i-n1KWo7fqlueLk7sm7cXAuD6sZsctq1f-LvADgAWk4qPGsdOEMCSbFZsfJT5R3r-sr2wtfVaFUX6J7B61Xkatr1xd75C8TpyWgFPVfAIAMWC";
            String requestUrl = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=" + accessToken;

            System.out.println("========templateSendReq=========" + templateSendRequestDto.toJsonString());
            String request = HttpUtil.postRequest(requestUrl, templateSendRequestDto.toJsonString());

            JSONObject jb = JSONObject.parseObject(request);
            Object errmsg = jb.get("errmsg");
            if (!"ok".equals(errmsg)) {
                logger.info("=========request=========" + request);
                return "来电通知推送失败~~";
            }

            logger.info("=========request=========" + request);
        }

        return "来电通知推送成功~~";

    }


    /**
     * 删除操作记录列表
     *
     * @param operationId
     */
    @RequestMapping(value = "/delOperationRecord.do")
    @ResponseBody
    public void delOperationRecord(Integer operationId) {
        tSetService.delOperationRecordByOperationId(operationId);
    }


    /**
     * 緩存被叫號碼
     *
     * @param userId
     * @return
     */
    @RequestMapping(value = "/demo")
    @ResponseBody
    public String test(Integer userId) {
        TUserinfo tUserinfo = tUserinfoService.selectOpenidByUserId(userId);

        if (tUserinfo == null) {
            logger.info("demo.do user is not exitst. userId=" + userId);
            return "false";
        }

        String phoneNumber = tUserinfo.getPhonenumber();
        if (phoneNumber == null || phoneNumber.length() < 3) {
            logger.info("demo.do phoneNumber is error. userId=" + userId + ",phoneNumber=" + phoneNumber);
            return "false";
        }
        try {
            redisClient.set(14, "xbms_calleeid", phoneNumber);
            logger.info("demo.do success. userId=" + userId + ",phoneNumber=" + phoneNumber);

        } catch (Exception e) {
            logger.warn("demo.do error", e);
            return "false";
        }

        return "true";

    }


    /**
     * 推荐人查询
     *
     * @param userId
     * @return
     */
    @RequestMapping(value = "/recommendedByMeQry.do")
    @ResponseBody
    public RecommendResp recommendedbyMeQry(Integer userId) {
        RecommendResp resp = new RecommendResp();
        List<TUserinfoDto> tUserinfoDtos = new ArrayList<>();

        List<TUserinfo> tUserinfoList = tUserinfoService.selectBySonId(userId);
        logger.info("===========tUserinfoList={}============", tUserinfoList.size());

        for (TUserinfo tUserinfo : tUserinfoList) {

            TUserinfoDto tUserinfoDto = new TUserinfoDto();
            tUserinfoDto.setId(tUserinfo.getId());
            tUserinfoDto.setOpenId(tUserinfo.getOpenid());
            tUserinfoDto.setNickName(tUserinfo.getNickname());
            tUserinfoDto.setPhoneNumber(tUserinfo.getPhonenumber());
            tUserinfoDto.setRecommenderId(tUserinfo.getRecommenderId());
            tUserinfoDtos.add(tUserinfoDto);
        }

        resp.setRetCode(0);
        resp.setRetDesc("ok");
        resp.setRetData(tUserinfoDtos);
        return resp;

    }


    /**
     * 微信支付接口(开通会员)
     *
     * @param request
     * @param openid
     * @param goodsId
     * @return
     */
    @RequestMapping(value = "/wxPay_openMembership.do", method = RequestMethod.GET)
    @ResponseBody
    public Map openMembership(HttpServletRequest request, String openid, Integer goodsId) {
        TUserinfo tUserinfo = tUserinfoService.selectUserIdByOpenId(openid);
        TMeal tMeal = tMealService.selectBygoodsId(goodsId);
        BigDecimal decimal = new BigDecimal(tMeal.getPrice());
        BigDecimal yibai = new BigDecimal(100);
        BigDecimal bigDecimal = decimal.multiply(yibai);
        BigInteger bigInteger = bigDecimal.toBigInteger();

        logger.info("======tMeal.getPrice()={}=======", tMeal.getPrice());
        try {
            //获取请求ip地址
            String ip = request.getHeader("x-forwarded-for");
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            if (ip.indexOf(",") != -1) {
                String[] ips = ip.split(",");
                ip = ips[0].trim();
            }
            logger.info("=======ip={}=========", ip);

            String out_trade_no = WXPayUtil.generateNonceStr();
            logger.info("=======out_trade_no={}============", out_trade_no);
            //拼接统一下单地址参数
            Map<String, String> paraMap = new HashMap<String, String>();
            paraMap.put("appid", XBMSConstant.XBMS_WX_APPID);
            paraMap.put("body", "小兵秘书-开通会员支付");
            paraMap.put("mch_id", XBMSConstant.XBMS_WXPAY_MCH_ID);
            paraMap.put("nonce_str", WXPayUtil.generateNonceStr());
            paraMap.put("openid", openid);
            paraMap.put("out_trade_no", out_trade_no);//订单号
            paraMap.put("spbill_create_ip", ip);
            paraMap.put("total_fee", bigInteger.toString());
            paraMap.put("trade_type", XBMSConstant.XBMS_WXPAY_TRADE_TYPE);
            paraMap.put("notify_url", "https://ai.yousayido.net/busiManagement/wxgzh/callback.do");// 此路径是微信服务器调用支付结果通知路径随意写

            logger.info("=======paraMap={}=========", paraMap.toString());

            //商户号的密钥
            String paternerKey = XBMSConstant.xbms_wxpay_paternerkey;
            //生成签名
            String sign = WXPayUtil.generateSignature(paraMap, paternerKey);
            paraMap.put("sign", sign);
            logger.info("========paraMap={}==========", paraMap.toString());

            String xml = WXPayUtil.mapToXml(paraMap);//将所有参数(map)转xml格式
            logger.info("========xml={}==========", xml);


            //生成预支付订单
            TOrder tOrder = new TOrder();
            tOrder.setUserId(tUserinfo.getId());
            tOrder.setOpenid(openid);
            tOrder.setGoodsId(goodsId);
            tOrder.setPrice(tMeal.getPrice());
            tOrder.setNumber(1);
            tOrder.setPayMoney(tMeal.getPrice());
            tOrder.setStatus(1);
            tOrder.setTradeNo(out_trade_no);
            int i = tOrderService.addOrder(tOrder);

            //统一下单url: https://api.mch.weixin.qq.com/pay/unifiedorder
            String unifiedorder_url = "https://api.mch.weixin.qq.com/pay/unifiedorder";

            String xmlStr = HttpRequest.sendPost(unifiedorder_url, xml);//发送post请求"统一下单接口"返回预支付id:prepay_id
            logger.info("========xmlStr={}==========", xmlStr);

            //以下内容是返回前端页面的json数据
            String prepay_id = "";//预支付id
            if (xmlStr.indexOf("SUCCESS") != -1) {

                Map<String, String> map = WXPayUtil.xmlToMap(xmlStr);
                prepay_id = map.get("prepay_id");

            }

            Map<String, String> payMap = new HashMap<String, String>();

            payMap.put("appId", XBMSConstant.XBMS_WX_APPID);
            payMap.put("timeStamp", System.currentTimeMillis() / 1000 + "");
            payMap.put("nonceStr", WXPayUtil.generateNonceStr());
            payMap.put("signType", "MD5");
            payMap.put("package", "prepay_id=" + prepay_id);

            String paySign = WXPayUtil.generateSignature(payMap, paternerKey);
            payMap.put("paySign", paySign);

            return payMap;

        } catch (Exception e) {
            e.printStackTrace();

        }
        return null;
    }


    /**
     * 微信支付接口(送秘书)
     *
     * @param request
     * @param openid
     * @param goodsId
     * @return
     */
    @RequestMapping(value = "/wxPay_giveSecretary.do", method = RequestMethod.GET)
    @ResponseBody
    public Map giveSecretary(HttpServletRequest request, String openid, Integer goodsId) {
        TUserinfo tUserinfo = tUserinfoService.selectUserIdByOpenId(openid);
        TMeal tMeal = tMealService.selectBygoodsId(goodsId);
        BigDecimal decimal = new BigDecimal(tMeal.getPrice());
        BigDecimal yibai = new BigDecimal(100);
        BigDecimal bigDecimal = decimal.multiply(yibai);
        BigInteger bigInteger = bigDecimal.toBigInteger();
        try {
            //获取请求ip地址
            String ip = request.getHeader("x-forwarded-for");
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            if (ip.indexOf(",") != -1) {
                String[] ips = ip.split(",");
                ip = ips[0].trim();
            }
            logger.info("=======ip={}=========", ip);

            String out_trade_no = WXPayUtil.generateNonceStr();
            //拼接统一下单地址参数
            Map<String, String> paraMap = new HashMap<String, String>();
            paraMap.put("appid", XBMSConstant.XBMS_WX_APPID);
            paraMap.put("body", "小兵秘书-送秘书支付");
            paraMap.put("mch_id", XBMSConstant.XBMS_WXPAY_MCH_ID);
            paraMap.put("nonce_str", WXPayUtil.generateNonceStr());
            paraMap.put("openid", openid);
            paraMap.put("out_trade_no", out_trade_no);//订单号
            paraMap.put("spbill_create_ip", ip);
            paraMap.put("total_fee", bigInteger.toString());
            paraMap.put("trade_type", XBMSConstant.XBMS_WXPAY_TRADE_TYPE);
            paraMap.put("notify_url", "https://ai.yousayido.net/busiManagement/wxgzh/giveSecretaryCallback.do");// 此路径是微信服务器调用支付结果通知路径随意写

            //商户号的密钥
            String paternerKey = XBMSConstant.xbms_wxpay_paternerkey;
            //生成签名
            String sign = WXPayUtil.generateSignature(paraMap, paternerKey);
            paraMap.put("sign", sign);
            logger.info("========paraMap={}==========", paraMap.toString());

            String xml = WXPayUtil.mapToXml(paraMap);//将所有参数(map)转xml格式
            logger.info("========xml={}==========", xml);


            //生成预支付订单
            TOrder tOrder = new TOrder();
            tOrder.setUserId(tUserinfo.getId());
            tOrder.setOpenid(openid);
            tOrder.setGoodsId(goodsId);
            tOrder.setPrice(tMeal.getPrice());
            tOrder.setNumber(1);
            tOrder.setPayMoney(tMeal.getPrice());
            tOrder.setStatus(1);
            tOrder.setTradeNo(out_trade_no);
            int i = tOrderService.addOrder(tOrder);


            //统一下单url: https://api.mch.weixin.qq.com/pay/unifiedorder
            String unifiedorder_url = "https://api.mch.weixin.qq.com/pay/unifiedorder";

            String xmlStr = HttpRequest.sendPost(unifiedorder_url, xml);//发送post请求"统一下单接口"返回预支付id:prepay_id
            logger.info("========xmlStr={}==========", xmlStr);

            //以下内容是返回前端页面的json数据
            String prepay_id = "";//预支付id
            if (xmlStr.indexOf("SUCCESS") != -1) {

                Map<String, String> map = WXPayUtil.xmlToMap(xmlStr);
                prepay_id = map.get("prepay_id");

            }

            Map<String, String> payMap = new HashMap<String, String>();

            payMap.put("appId", XBMSConstant.XBMS_WX_APPID);
            payMap.put("timeStamp", System.currentTimeMillis() / 1000 + "");
            payMap.put("nonceStr", WXPayUtil.generateNonceStr());
            payMap.put("signType", "MD5");
            payMap.put("package", "prepay_id=" + prepay_id);

            String paySign = WXPayUtil.generateSignature(payMap, paternerKey);
            payMap.put("paySign", paySign);

            return payMap;

        } catch (Exception e) {
            e.printStackTrace();

        }
        return null;
    }


    /**
     * 微信支付回调接口（开通会员）
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/callback.do")
    @ResponseBody
    public String callBack(HttpServletRequest request, HttpServletResponse response) {

        logger.info("========进入回调接口==========");

        InputStream is = null;
        try {
            //获取请求的流信息(这里是微信发的xml格式所有只能使用流来读)
            is = request.getInputStream();
            String xml = iStreamToXML(is);

            //将微信发的xml转map
            Map<String, String> notifyMap = WXPayUtil.xmlToMap(xml);

            if (notifyMap.get("return_code").equals("SUCCESS")) {

                if (notifyMap.get("result_code").equals("SUCCESS")) {

                    //商户订单号
                    String ordersSn = notifyMap.get("out_trade_no");

                    //实际支付的订单金额:单位 分
                    String amountpaid = notifyMap.get("total_fee");

                    //将分转换成元-实际支付金额:元
                    BigDecimal amountPay = (new BigDecimal(amountpaid).divide(new BigDecimal("100"))).setScale(2);

                    String openid = notifyMap.get("openid");

                    String trade_type = notifyMap.get("trade_type");
                    logger.info("amountpaid:" + amountpaid);
                    logger.info("amountPay:" + amountPay);
                    logger.info("ordersSn:" + ordersSn);
                    logger.info("openid:" + openid);
                    logger.info("trade_type:" + trade_type);

                    //支付成功，更新支付状态
                    tOrderService.updateOrderStatus(ordersSn);

                    //插入会员信息
                    TMeal tMeal = tMealService.selectByTradeNo(ordersSn);
                    Integer useDays = tMeal.getUseDays();
                    if ("私人秘书".equals(tMeal.getType())) {
                        tUserinfoService.updateMemberinfo(openid, useDays);
                    }
                    if ("私人定制".equals(tMeal.getType())) {
                        tUserinfoService.updateMemberInfo(openid, useDays);
                    }

                }

            }

            //告诉微信服务器收到信息了，不要在调用回调action了========这里很重要回复微信服务器信息用流发送一个xml即可
            response.getWriter().write("<xml><return_code><![CDATA[SUCCESS]]></return_code></xml>");
            is.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    /**
     * 微信支付回调接口（送秘书）
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/giveSecretaryCallback.do")
    @ResponseBody
    public TQctivationcode giveSecretaryCallback(HttpServletRequest request, HttpServletResponse response) {

        InputStream is = null;
        String ordersSn = "";
        try {
            //获取请求的流信息(这里是微信发的xml格式所有只能使用流来读)
            is = request.getInputStream();
            String xml = iStreamToXML(is);

            //将微信发的xml转map
            Map<String, String> notifyMap = WXPayUtil.xmlToMap(xml);

            if (notifyMap.get("return_code").equals("SUCCESS")) {

                if (notifyMap.get("result_code").equals("SUCCESS")) {

                    //商户订单号
                    ordersSn = notifyMap.get("out_trade_no");

                    //实际支付的订单金额:单位 分
                    String amountpaid = notifyMap.get("total_fee");

                    //将分转换成元-实际支付金额:元
                    BigDecimal amountPay = (new BigDecimal(amountpaid).divide(new BigDecimal("100"))).setScale(2);

                    String openid = notifyMap.get("openid");

                    String trade_type = notifyMap.get("trade_type");
                    logger.info("amountpaid:" + amountpaid);
                    logger.info("amountPay:" + amountPay);
                    logger.info("ordersSn:" + ordersSn);
                    logger.info("openid:" + openid);
                    logger.info("trade_type:" + trade_type);

                    //更新支付状态
                    tOrderService.updateOrderStatus(ordersSn);
                    //生成激活码，插入db
                    TOrder order = tOrderService.selectByTradeNo(ordersSn);

                    /*TMeal tMeal = tMealService.selectBygoodsId(order.getGoodsId());
                    Integer useDays = tMeal.getUseDays();*/

                    TQctivationcode code = new TQctivationcode();
                    String activationCode = RandomUtils.genActivationCode();
                    TQctivationcode tQctivationcode = tQctivationcodeService.selectByCode(activationCode);
                    if (StringUtils.isEmpty(tQctivationcode)) {
                        code.setCode(activationCode);
                    }
                    code.setCodeMealId(order.getGoodsId() + "");
                    code.setAgentId(order.getUserId());
                    code.setStatus(1);
                    code.setCodeType("USER_BUY");
                    code.setTradeNo(ordersSn);
                    tQctivationcodeService.addTQctivationcode(code);

                    logger.info("=======code={}=======", code);

                }

            }

            //告诉微信服务器收到信息了，不要在调用回调action了========这里很重要回复微信服务器信息用流发送一个xml即可
            response.getWriter().write("<xml><return_code><![CDATA[SUCCESS]]></return_code></xml>");
            is.close();

            TQctivationcode tQctivationcode = tQctivationcodeService.selectByTradeNo(ordersSn);
            logger.info("=========tQctivationcode={}============", tQctivationcode);
            return tQctivationcode;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    /**
     * 套餐查询
     *
     * @return
     */
    @RequestMapping(value = "/mealListQry.do")
    @ResponseBody
    public List<TMeal> mealListQry() {
        List<TMeal> tMeals = new ArrayList<>();
        List<TMeal> tMealList = tMealService.selectMealList();
        if (!CollectionUtils.isEmpty(tMealList)) {
            for (TMeal meal : tMealList) {
                TMeal tMeal = new TMeal();
                tMeal.setId(meal.getId());
                tMeal.setPrice(meal.getPrice());
                tMeal.setType(meal.getType());
                tMeal.setCreateTime(new Date());
                tMeal.setName(meal.getName());
                tMeal.setOriginalPrice(meal.getOriginalPrice());
                tMeal.setUseDays(meal.getUseDays());
                tMeals.add(tMeal);
            }
            return tMeals;
        }
        return null;
    }


    /**
     * 查询是否会员
     *
     * @return
     */
    @RequestMapping(value = "/isMembershipQry.do")
    @ResponseBody
    public UserIsMembership isMembershipQry(String openid) {
        UserIsMembership membership = new UserIsMembership();
        TUserinfo tUserinfo = tUserinfoService.selectUserIdByOpenId(openid);
        if (StringUtils.isEmpty(tUserinfo)) {
            return new UserIsMembership("此用户不存在");
        }

        membership.setUserId(tUserinfo.getId());
        membership.setNickname(tUserinfo.getNickname());
        membership.setOpenid(tUserinfo.getOpenid());
        membership.setIsMembership(tUserinfo.getIsMembership());
        membership.setExpireTime(tUserinfo.getExpireTime());
        return membership;
    }


    /**
     * 激活码开通会员
     *
     * @return
     */
    @RequestMapping(value = "/openMembershipByCode.do")
    @ResponseBody
    public CheckSmsCodeResp openMembershipByCode(String code, String openid) {
        TUserinfo tUserinfo = tUserinfoService.selectUserIdByOpenId(openid);
        CheckSmsCodeResp resp = new CheckSmsCodeResp();

        int i = tQctivationcodeService.openMembershipByCode(code, tUserinfo.getId());
        if (i < 1) {
            resp.setRetCode(1);
            resp.setRetDesc("开通会员失败");
            return resp;
        }
        TMeal tMeal = tMealService.selectByCodeMealId(code);
        String tMealType = tMeal.getType();
        Integer useDays = tMeal.getUseDays();
        logger.info("========tMealName={}==========", tMealType);
        if ("私人定制".equals(tMealType)) {
            tUserinfoService.updateMemberInfo(openid, useDays);
        }
        if ("私人秘书".equals(tMealType)) {
            tUserinfoService.updateMemberinfo(openid, useDays);
        }

        resp.setRetCode(0);
        resp.setRetDesc("开通会员成功");
        return resp;
    }


    /**
     * 添加组织机构申请
     *
     * @param name
     * @param openId
     * @param picFacade
     * @param picBack
     * @param status
     * @return
     */
    @RequestMapping(value = "/orgApplyAdd.do")
    @ResponseBody
    public CheckSmsCodeResp orgApplyAdd(String name, String openId,
                                        @RequestParam("picFacade") MultipartFile picFacade,
                                        @RequestParam("picBack") MultipartFile picBack,
                                        Integer status) {
        CheckSmsCodeResp resp = new CheckSmsCodeResp();
        if (picBack.isEmpty() || picFacade.isEmpty()) {
            resp.setRetCode(2);
            resp.setRetDesc("图片不能为空");
            return resp;
        }
        String facade = picFacade.getOriginalFilename();
        String back = picBack.getOriginalFilename();

        //ftp将图片上传到服务器
        String ftpPath = "tm/xbms/orgimage/";
        InputStream is1 = null;
        InputStream is2 = null;
        try {
            is1 = picFacade.getInputStream();
            is2 = picBack.getInputStream();
            FTPClient ftpClient = FTPUtils.getFTPClient(address, port, ftpName, password, "UTF-8");
            boolean isSuccess = ftpClient.storeFile(facade, is1);
            boolean isSuccess2 = ftpClient.storeFile(back, is2);
            FTPUtils.uploadFile(ftpClient, ftpPath, facade, is1);
            FTPUtils.uploadFile(ftpClient, ftpPath, back, is2);
            logger.info("=====isSuccess={}======isSuccess2={}==========", isSuccess, isSuccess2);
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*facade = "/mnt/tm/xbms/orgimage/" + facade;
        back = "/mnt/tm/xbms/orgimage/" + back;

        try {
            picFacade.transferTo(new File(facade));
            picBack.transferTo(new File(back));
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        TMechanism tMechanism = new TMechanism();
        String code = RandomUtils.genSixcode();
        String orgNum = RandomUtils.genEightOrgnum();
        logger.info("=======code={}===orgNum={}=========", code, orgNum);
        TMechanism mechanism = tMechanismService.selectByOrgNum(code, orgNum);
        if (StringUtils.isEmpty(mechanism)) {
            tMechanism.setCode(code);
            tMechanism.setOrgNum(orgNum);
        }

        tMechanism.setName(name);
        tMechanism.setOpenid(openId);
        tMechanism.setPicBack(back);
        tMechanism.setPicFacade(facade);
        tMechanism.setStatus(2);

        int i = tMechanismService.insertOrgApply(tMechanism);
        if (i < 1) {
            resp.setRetCode(1);
            resp.setRetDesc("组织机构添加失败");
            return resp;
        }

        tUserinfoService.updateIdeByOpenid(openId);
        resp.setRetCode(0);
        resp.setRetDesc("组织机构添加成功");
        return resp;
    }


    /**
     * 申请代理
     *
     * @param code
     * @param empNum
     * @param openid
     * @return
     */
    @RequestMapping(value = "/agentAdd.do")
    @ResponseBody
    public CheckSmsCodeResp agentAdd(String code, String empNum, String openid) {
        CheckSmsCodeResp resp = new CheckSmsCodeResp();

        TUserinfo tUserinfo = tUserinfoService.selectUserIdByOpenId(openid);
        if (!StringUtils.isEmpty(tUserinfo.getRecommenderId())) {
            resp.setRetCode(1);
            resp.setRetDesc("您已经是代理，无法申请");
            return resp;
        }

        TUserinfo userinfo = tUserinfoService.selectByCode(code);
        if (StringUtils.isEmpty(userinfo)) {
            resp.setRetCode(1);
            resp.setRetDesc("机构码不存在");
            return resp;
        }
        Integer id = userinfo.getId();
        String sonId = "-0-" + id;
        tUserinfoService.updateEmpByOpenid(id, sonId, empNum, openid);

        resp.setRetCode(0);
        resp.setRetDesc("申请代理成功");
        return resp;
    }


    /**
     * 查询是否为合伙人和代理
     *
     * @param openid
     * @return
     */
    @RequestMapping(value = "/isAgentQry.do")
    @ResponseBody
    public IsAgentResp isAgentQry(String openid) {
        IsAgentResp resp = new IsAgentResp();
        TUserinfo tUserinfo = tUserinfoService.selectUserIdByOpenId(openid);
        logger.info("=========tUserinfo==========" + tUserinfo);
        if (tUserinfo.getIdentity() == null) {
            resp.setOpenid(openid);
            resp.setNickname(tUserinfo.getNickname());
            resp.setIdentity(tUserinfo.getIdentity());
            return resp;
        }
        if (!"2".equals(tUserinfo.getIdentity().toString())) {
            logger.info("=======come in=========");
            resp.setOpenid(openid);
            resp.setNickname(tUserinfo.getNickname());
            resp.setIdentity(tUserinfo.getIdentity());
            return resp;
        }
        TUserinfo userinfo = tUserinfoService.selectOpenidByUserId(tUserinfo.getRecommenderId());

        TMechanism tMechanism = tMechanismService.selectByOpenId(userinfo.getOpenid());

        resp.setOpenid(openid);
        resp.setNickname(tUserinfo.getNickname());
        resp.setIdentity(tUserinfo.getIdentity());
        resp.setOrgNum(tMechanism.getOrgNum());
        return resp;
    }


    /**
     * 查询机构码和状态
     *
     * @param openId
     * @return
     */
    @RequestMapping(value = "/orgNumAndStatusQry.do")
    @ResponseBody
    public TMechanism orgNumAndStatusQry(String openId) {
        TMechanism mechanism = new TMechanism();
        TMechanism tMechanism = tMechanismService.selectByOpenId(openId);
        if (!StringUtils.isEmpty(tMechanism)) {
            mechanism.setName(tMechanism.getName());
            mechanism.setCode(tMechanism.getCode());
            mechanism.setStatus(tMechanism.getStatus());
            return mechanism;
        }
        return null;
    }


    /**
     * 查询会员过期时间
     *
     * @param openid
     * @return
     */
    @RequestMapping(value = "/membershipListQry.do")
    @ResponseBody
    public CheckSmsCodeResp membershipListQry(String openid) {
        CheckSmsCodeResp resp = new CheckSmsCodeResp();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        TUserinfo tUserinfo = tUserinfoService.selectUserIdByOpenId(openid);
        if (StringUtils.isEmpty(tUserinfo)) {
            resp.setRetCode(1);
            resp.setRetDesc("没有此用户");
            return resp;
        }
        if (StringUtils.isEmpty(tUserinfo.getIsMembership())) {
            resp.setRetCode(1);
            resp.setRetDesc("普通用户");
            return resp;
        }
        resp.setRetCode(0);
        resp.setRetDesc(sdf.format(tUserinfo.getExpireTime()));
        return resp;
    }


    /**
     * 查询用户的历史订单
     *
     * @param openid
     * @return
     */
    @RequestMapping(value = "/orderHistoryQry.do")
    @ResponseBody
    public OrderHistoryResp orderHistoryQry(String openid) {
        OrderHistoryResp resp = new OrderHistoryResp();
        List<OrderHistory> orderHistories = new ArrayList<>();
        List<OrderHistory> codeOrders = new ArrayList<>();
        //无激活码订单
        List<OrderHistory> orderHistoryList = tOrderService.selectOrderHistoryByOpenid(openid);
        logger.info("========orderHistoryList={}==========", orderHistoryList.size());
        if (!StringUtils.isEmpty(orderHistoryList)) {
            for (OrderHistory order : orderHistoryList) {
                OrderHistory orderHistory1 = new OrderHistory();
                orderHistory1.setOpenid(order.getOpenid());
                orderHistory1.setMealName(order.getMealName());
                orderHistory1.setCreateTime(order.getCreateTime());
                orderHistory1.setNumber(order.getNumber());
                orderHistory1.setPayMoney(order.getPayMoney());
                orderHistory1.setPayTime(order.getPayTime());
                orderHistory1.setPrice(order.getPrice());
                orderHistory1.setStatus(order.getStatus());
                orderHistory1.setTradeNo(order.getTradeNo());

                orderHistories.add(orderHistory1);
            }
        }
        logger.info("========orderHistories={}==========", orderHistories.size());

        //激活码订单
        List<OrderHistory> codeOrderList = tOrderService.selectCodeOrderByOpenid(openid);
        logger.info("========codeOrderList={}==========", codeOrderList.size());
        if (!StringUtils.isEmpty(codeOrderList)) {
            for (OrderHistory order : codeOrderList) {
                OrderHistory orderHistory = new OrderHistory();
                orderHistory.setOpenid(order.getOpenid());
                orderHistory.setMealName(order.getMealName());
                orderHistory.setCreateTime(order.getCreateTime());
                orderHistory.setNumber(order.getNumber());
                orderHistory.setPayMoney(order.getPayMoney());
                orderHistory.setPayTime(order.getPayTime());
                orderHistory.setPrice(order.getPrice());
                orderHistory.setStatus(order.getStatus());
                orderHistory.setTradeNo(order.getTradeNo());
                orderHistory.setCode(order.getCode());

                codeOrders.add(orderHistory);
            }
        }
        orderHistories.addAll(codeOrders);
        logger.info("========orderHistories={}==========", orderHistories.size());

        resp.setRetCode(0);
        resp.setRetDesc("历史订单如下（包含激活码）");
        resp.setRetData(orderHistories);
        return resp;
    }


    /**
     * 添加用户改进建议
     *
     * @param openid
     * @param sugFuntion
     * @param sugCraft
     * @param sugOther
     * @return
     */
    @RequestMapping(value = "/suggestionAdd.do")
    @ResponseBody
    public CheckSmsCodeResp suggestionAdd(String openid,
                                          String sugFuntion,
                                          String sugCraft,
                                          String sugOther) {
        CheckSmsCodeResp resp = new CheckSmsCodeResp();
        TSuggestion suggestion = new TSuggestion();
        suggestion.setOpenid(openid);
        suggestion.setSugFuntion(sugFuntion);
        suggestion.setSugCraft(sugCraft);
        suggestion.setSugOther(sugOther);
        int i = tSuggestionService.insertSuggestion(suggestion);
        if (i < 1) {
            resp.setRetCode(1);
            resp.setRetDesc("改进建议添加失败");
            return resp;
        }

        resp.setRetCode(0);
        resp.setRetDesc("改进建议添加成功");
        return resp;
    }


    /**
     * 通过code，点击链接获得用户的基本信息，并存到db
     *
     * @param code
     * @return
     */
    @RequestMapping(value = "/wxUserinfo")
    @ResponseBody
    public TUserinfo wxUserinfoDetail(String code, String parentId) {
        Integer parentid = null;
        String sonId = "";
        String sonIds = "";

        if (!StringUtils.isEmpty(parentId)) {
            parentid = Integer.parseInt(parentId);
        }

        if (!StringUtils.isEmpty(parentId)) {
            //26
            int userId = Integer.parseInt(parentId);
            TUserinfo tUserinfo = tUserinfoService.selectOpenidByUserId(userId);
            //-0-21
            sonId = tUserinfo.getSonId();
            if (sonId == null) {
                sonIds = "-0-" + userId;
            } else {
                sonIds = sonId + "-" + userId;
            }
        }

        String url = "https://api.weixin.qq.com/sns/oauth2/access_token?" +
                "appid=" + XBMSConstant.XBMS_WX_APPID +
                "&secret=" + XBMSConstant.XBMS_WX_SECRET +
                "&code=" + code +
                "&grant_type=authorization_code";
        String postRequest = HttpUtil.getHttps(url);
        logger.info("=========postRequest=========" + postRequest);

        JSONObject object = JSONObject.parseObject(postRequest);
        String openid = object.getString("openid");
        String acctoken = object.getString("access_token");
        logger.info("=========openid=========" + openid);
        // 获取用户的信息
        String userUrl = "https://api.weixin.qq.com/sns/userinfo?access_token=" + acctoken + "&openid=" + openid + "&lang=zh_CN";
        String info = HttpUtil.getHttps(userUrl);
        // 把信息保存到数据库
        JSONObject userInfo = JSONObject.parseObject(info);
        //  {"openid":"opNfewkh_P3y7ya0rlw7FXbwwzrQ","nickname":"橘子橙子大柚子","sex":1,"language":"zh_CN","city":"深圳","province":"广东","country":"中国","headimgurl":"http:\/\/thirdwx.qlogo.cn\/mmopen\/vi_32\/1USguvJx0N2v4gGpDu6Ypiajiae2AsL7D8ibwwicroN7JQRSbNJGWudx0qHZefg95deichu0DFeMgxicQmgDVDeSSkXg\/132","privilege":[]}

        TUserinfo user = new TUserinfo();

        user.setOpenid(openid);

        String nickname = userInfo.getString("nickname");
        user.setNickname(nickname);

        String sex = userInfo.getString("sex");
        user.setSex(sex);

        String language = userInfo.getString("language");
        user.setLanguage(language);

        String city = userInfo.getString("city");
        user.setCity(city);

        String province = userInfo.getString("province");
        user.setProvince(province);


        String country = userInfo.getString("country");
        user.setCountry(country);

        String headimgurl = userInfo.getString("headimgurl");
        user.setHeadimgurl(headimgurl);

        user.setRecommenderId(parentid);
        user.setSonId(sonIds);

        TUserinfo dbUser = tUserinfoService.selectUserIdByOpenId(openid);
        if (dbUser == null) {
            tUserinfoService.insert(user);

            TUserinfo tUserinfo = tUserinfoService.selectUserIdByOpenId(openid);
            tMallService.insertBaseData(tUserinfo.getId());

            TUserinfo userinfo = new TUserinfo();
            userinfo.setOpenid(openid);
            userinfo.setNickname(nickname);
            userinfo.setHeadimgurl(headimgurl);
            return userinfo;
        }

        return dbUser;
    }


    /**
     * 生成二维码邀请好友
     *
     * @param userId
     * @param url
     * @param httpServletResponse
     */
    @RequestMapping(value = "/getQrcode.do", method = RequestMethod.GET)
    @ResponseBody
    public void getQrcode(String userId, String url, HttpServletResponse httpServletResponse) {
        InputStream imagein = null;
        try {
            httpServletResponse.setContentType("application/x-png");
            httpServletResponse.addHeader("Content-Disposition",
                    "attachment;filename="
                            + URLEncoder.encode("图片.png", "utf-8"));
            OutputStream outputStream = httpServletResponse.getOutputStream();
//            String text = "https://ai.yousayido.net/wxgzh/templates/n-paymeinvite.html?parentId="+ids; // 二维码内容PPNkkUNC4DNCQuDuOv
            String text = url + "?parentId=" + userId;
            String pressText = "";//向享图片中添加文字
            int width = 300; // 二维码图片宽度
            int height = 300; // 二维码图片高度
            String format = "png";// 二维码的图片格式
            int fontStyle = 0; //字体风格
            Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>();
            hints.put(EncodeHintType.CHARACTER_SET, "utf-8"); // 内容所使用字符集编码
            BitMatrix bitMatrix = deleteWhite(new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height, hints));
            //生成的二维码
            BufferedImage buffImg = MatrixToImageWriter.toBufferedImage(bitMatrix);
            Graphics gs = buffImg.createGraphics();
            //设置画笔的颜色
            gs.setColor(Color.black);
            //设置字体
            Font font = new Font("宋体", fontStyle, 24);
            FontMetrics metrics = gs.getFontMetrics(font);
            //文字在图片中的坐标 这里设置在中间
            int startX = (width - metrics.stringWidth(pressText)) / 2;
            int startY = height - 400;
            gs.setFont(font);
            gs.drawString(pressText, startX, startY);
            //背景图片的路径
            imagein = this.getClass().getResourceAsStream("/2.png");
//            File logoFile = new File(WxController.class.getClassLoader().getResource("2.png").getFile());
//            logger.info("=======logoFile={}=========",logoFile.toString());
//            imagein = new FileInputStream(logoFile.toString());
            //读出背景图片
            BufferedImage image = ImageIO.read(imagein);
            Graphics g = image.getGraphics();
            //将二维码画到背景图片中
            g.drawImage(buffImg, 235, 365, buffImg.getWidth(), buffImg.getHeight(), null);
            // 生成二维码
            ImageIO.write(image, format, outputStream);
            ImageIO.write(image, format, new File("d:/NO." + userId + ".png"));

            outputStream.flush();
//            inputStream.close();
            outputStream.close();
            System.out.println("完成二维码生成");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("打包二维码异常");
        }
    }

    /**
     * 删除白边
     */
    private static BitMatrix deleteWhite(BitMatrix matrix) {
        int[] rec = matrix.getEnclosingRectangle();
        int resWidth = rec[2];
        int resHeight = rec[3];
        BitMatrix resMatrix = new BitMatrix(resWidth, resHeight);
        resMatrix.clear();
        for (int i = 0; i < resWidth; i++) {
            for (int j = 0; j < resHeight; j++) {
                if (matrix.get(i + rec[0], j + rec[1]))
                    resMatrix.set(i, j);
            }
        }
        return resMatrix;
    }


    public static String iStreamToXML(InputStream inputStream) {
        String str = "";
        try {
            StringBuilder sb = new StringBuilder();
            String line;

            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            str = sb.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return str;
    }


    public void post(HttpServletRequest request, HttpServletResponse response) {
        try {
            request.setCharacterEncoding("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
        }
        response.setContentType("text/html;charset=UTF-8");

        // 调用核心业务类接收消息、处理消息
        String respMessage = messageService.newMessageRequest(request);

        // 响应消息
        PrintWriter out = null;
        try {
            out = response.getWriter();
            out.print(respMessage);
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
        } finally {
            out.close();
            out = null;
        }
    }
}
