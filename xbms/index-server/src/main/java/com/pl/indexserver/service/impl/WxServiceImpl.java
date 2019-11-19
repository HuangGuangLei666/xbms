package com.pl.indexserver.service.impl;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Date;
import java.util.Map;

import com.pl.mapper.TMallMapper;
import com.pl.model.wx.XBMSConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.pl.indexserver.service.WxService;
import com.pl.indexserver.untils.HttpUtil;
import com.pl.indexserver.untils.RedisClient;
import com.pl.mapper.TUserinfoMapper;
import com.pl.model.wx.TUserinfo;


/**
 * @author HGL
 * @Date 2018/12/28
 */
@Service
public class WxServiceImpl implements WxService {
    private static final Logger logger = LoggerFactory.getLogger(WxServiceImpl.class);


    @Value("${xiaobingsecretary.AppId}")
    private String appId;
    @Value("${xiaobingsecretary.AppSecret}")
    private String appSecret;
    @Autowired
    private RedisClient redisClient;

    @Autowired
    private TUserinfoMapper tUserinfoMapper;
    @Autowired
    private TMallMapper tMallMapper;

    @Override
    public TUserinfo selectUserByPhoneNumber(String phoneNumber) {
        return tUserinfoMapper.selectUserByPhoneNumber(phoneNumber);
    }

    @Override
    public TUserinfo selectUserByOpenId(String openid) {
        /*try {
            String media_id = upload();
            logger.info("=========media_id={}============",media_id);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }*/
        return tUserinfoMapper.selectUserByOpenId(openid);
    }

    @Override
    public int updateByPrimaryKeySelective(int id) {
        return tUserinfoMapper.updateByPrimaryKeySelective(id);
    }

    @Override
    public int updateByPrimaryKey(Integer id, String phoneNumber, Integer recommenderId, String sonIds) {
        return tUserinfoMapper.updateByPrimaryKey(id, phoneNumber,recommenderId,sonIds);
    }

    @Override
    public String getMediaId() {
        String mediaId = "";
        try {
            mediaId = upload();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        return mediaId;
    }

    private String generateAccessToken() {

        try {
            String url = "https://api.weixin.qq.com/cgi-bin/token?" +
                         "grant_type=client_credential" +
                         "&appid=" + XBMSConstant.XBMS_WX_APPID +
                         "&secret=" + XBMSConstant.XBMS_WX_SECRET;
            String ret = HttpUtil.getHttps(url);
            //{"access_token":"25_SMumCkkqWQTjrcH4hFa-lkq7b6SCb9eE5dye9dGp3-ytOsIQ_u8jxk598dGtuCXn7UdV2a8Pfa_3u1xMTY0o4m-RsodVmWScXhvfGIZxQP4n89VuE1kzmRZkKt_yqsKm7DgrKdJz8NWCpKIdJDFfADAYSU","expires_in":7200}
            JSONObject obj = JSONObject.parseObject(ret);
            String accessToken = obj.getString("access_token");

            redisClient.set("accessToken", accessToken);
            redisClient.set("accessTokenDate", System.currentTimeMillis() + "");
            logger.info("====get new accesstoken=" + accessToken);
            return accessToken;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            logger.warn("generateAccessToken error.", e);
        }
        return null;
    }

    public String getAccessToken() {
        try {
            String accessToken = redisClient.get("accessToken");
            String accessTokenDate = redisClient.get("accessTokenDate");

            if (accessToken == null || accessToken.length() < 5 || accessTokenDate == null || accessTokenDate.length() < 5) {
                logger.info("accessToken or accessTokenDate is null.reflushAccessToken() ");
                accessToken = generateAccessToken();
                return accessToken;
            }
            long accessTokenDateLong = Long.parseLong(accessTokenDate);
            if (System.currentTimeMillis() - accessTokenDateLong > 3600000) {
                accessToken = generateAccessToken();
            }
            logger.debug("getAccessToken accessToken=" + accessToken);
            return accessToken;
        } catch (Exception e) {
            logger.warn("getAccessToken error.", e);
        }
        return null;
    }


    /**
     * {
     "subscribe": 1,
     "openid": "oPHkiwj9faf3D3o0mtuwgR9lGryo",
     "nickname": "老滢子.avi",
     "sex": 1,
     "language": "zh_CN",
     "city": "广州",
     "province": "广东",
     "country": "中国",
     "headimgurl": "http://thirdwx.qlogo.cn/mmopen/ZVWXzDLhdGbWxhOOCZDPuMMB3GzYGlvRvXnhgrT4aR4qvP6hsKt3s8ysoGh4VYn9Wib8ydlrJn5LOfHx168TAFeMbPglgh7E5/132",
     "subscribe_time": 1568601747,
     "remark": "",
     "groupid": 0,
     "tagid_list": [ ],
     "subscribe_scene": "ADD_SCENE_QR_CODE",
     "qr_scene": 0,
     "qr_scene_str": ""
     }
     * @param map
     */
    public void subscribe(Map<String, String> map) {
        String openid = map.get("FromUserName");
        String accessToken = getAccessToken();
        String url = "https://api.weixin.qq.com/cgi-bin/user/info?" +
                "access_token=" + accessToken +
                "&openid=" + openid;

        String ret = HttpUtil.getHttps(url);
        //{"access_token":"25_SMumCkkqWQTjrcH4hFa-lkq7b6SCb9eE5dye9dGp3-ytOsIQ_u8jxk598dGtuCXn7UdV2a8Pfa_3u1xMTY0o4m-RsodVmWScXhvfGIZxQP4n89VuE1kzmRZkKt_yqsKm7DgrKdJz8NWCpKIdJDFfADAYSU","expires_in":7200}
        JSONObject obj = JSONObject.parseObject(ret);

        TUserinfo user = new TUserinfo();

        user.setOpenid(openid);
        String subscribe = obj.getString("subscribe");
        user.setSubscribe(subscribe);

        String nickname = obj.getString("nickname");
        user.setNickname(nickname);

        String sex = obj.getString("sex");
        user.setSex(sex);

        String language = obj.getString("language");
        user.setLanguage(language);

        String city = obj.getString("city");
        user.setCity(city);

        String province = obj.getString("province");
        user.setProvince(province);


        String country = obj.getString("country");
        user.setCountry(country);

        String headimgurl = obj.getString("headimgurl");
        user.setHeadimgurl(headimgurl);

        String subscribeTime = obj.getString("subscribe_time");

        user.setSubscribeTime(new Date(1000 * Long.parseLong(subscribeTime)));

        String remark = obj.getString("remark");
        user.setRemark(remark);

        int groupid = obj.getInteger("groupid");
        user.setGroupid(groupid);

        String tagidList = obj.getString("tagid_list");
        user.setTagidList(tagidList);

        String subscribeScene = obj.getString("subscribe_scene");
        user.setSubscribeScene(subscribeScene);

        String qrScene = obj.getString("qr_scene");
        user.setQrScene(qrScene);

        String qrSceneStr = obj.getString("qr_scene_str");
        user.setQrSceneStr(qrSceneStr);

        user.setCreateTime(new Date());

        TUserinfo dbUser = tUserinfoMapper.selectByPrimaryKey(openid);
        if (dbUser == null) {
            tUserinfoMapper.insert(user);
            logger.info("insert user success. obj=" + obj);

            TUserinfo tUserinfo = tUserinfoMapper.selectUserIdByOpenId(openid);
            tMallMapper.insertBaseData(tUserinfo.getId());
        } else {
            //TUserinfo updUser = new TUserinfo();
            logger.info("user alread exists. obj=" + obj);
        }
    }


    public void unsubscribe(Map<String, String> map) {
        logger.info("unsubscribe. map=" + map);
    }


    /**
     * 获取 media_id
     //     * @param filePath
     //     * @param accessToken
     //     * @param type
     * @return
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws KeyManagementException
     */
    public String upload() throws IOException, NoSuchAlgorithmException, NoSuchProviderException, KeyManagementException {
        String filePath = "C:\\Users\\Pulan\\Desktop\\假笑男孩.jpg";
        String accessToken = getAccessToken();
        String type = "image";

        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new IOException("文件不存在");
        }
        String uploadUrl = "https://api.weixin.qq.com/cgi-bin/media/upload?access_token=ACCESS_TOKEN&type=TYPE";

        String url = uploadUrl.replace("ACCESS_TOKEN", accessToken).replace("TYPE",type);

        URL urlObj = new URL(url);
        //连接
        HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();

        con.setRequestMethod("POST");
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);

        //设置请求头信息
        con.setRequestProperty("Connection", "Keep-Alive");
        con.setRequestProperty("Charset", "UTF-8");

        //设置边界
        String BOUNDARY = "----------" + System.currentTimeMillis();
        con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);

        StringBuilder sb = new StringBuilder();
        sb.append("--");
        sb.append(BOUNDARY);
        sb.append("\r\n");
        sb.append("Content-Disposition: form-data;name=\"file\";filename=\"" + file.getName() + "\"\r\n");
        sb.append("Content-Type:application/octet-stream\r\n\r\n");

        byte[] head = sb.toString().getBytes("utf-8");

        //获得输出流
        OutputStream out = new DataOutputStream(con.getOutputStream());
        //输出表头
        out.write(head);

        //文件正文部分
        //把文件已流文件的方式 推入到url中
        DataInputStream in = new DataInputStream(new FileInputStream(file));
        int bytes = 0;
        byte[] bufferOut = new byte[1024];
        while ((bytes = in.read(bufferOut)) != -1) {
            out.write(bufferOut, 0, bytes);
        }
        in.close();

        //结尾部分
        byte[] foot = ("\r\n--" + BOUNDARY + "--\r\n").getBytes("utf-8");//定义最后数据分隔线

        out.write(foot);

        out.flush();
        out.close();

        StringBuffer buffer = new StringBuffer();
        BufferedReader reader = null;
        String result = null;
        try {
            //定义BufferedReader输入流来读取URL的响应
            reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            if (result == null) {
                result = buffer.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        JSONObject jsonObj = JSONObject.parseObject(result);
        System.out.println(jsonObj);
        String typeName = "media_id";
        if(!"image".equals(type)){
            typeName = type + "_media_id";
        }
        String mediaId = jsonObj.getString(typeName);
        return mediaId;
    }

}
