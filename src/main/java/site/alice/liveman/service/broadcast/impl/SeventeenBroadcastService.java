/*
 * <Alice LiveMan>
 * Copyright (C) <2018>  <NekoSunflower>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package site.alice.liveman.service.broadcast.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import site.alice.liveman.model.AccountInfo;
import site.alice.liveman.service.broadcast.BroadcastService;
import site.alice.liveman.utils.HttpRequestUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class SeventeenBroadcastService implements BroadcastService {

    private static final String API_RTMP                = "https://api-dsa.17app.co/api/v1/rtmp";
    private static final String API_17APP_GATEWAY       = "https://api-dsa.17app.co/apiGateWay";
    private static final String API_17WINWINSUN_GATEWAY = "https://api-17.winwinsun.com/apiGateWay";

    @Override
    public boolean isMatch(String accountSite) {
        return "17live".equals(accountSite);
    }

    @Override
    public String getBroadcastAddress(AccountInfo accountInfo) throws Exception {
        Map<String, String> requestHeader = buildRequestHeader(accountInfo);
        String rtmpJSON = HttpRequestUtil.downloadUrl(new URI(API_RTMP), null, "{\"streamType\":\"rtmp\",\"eventID\":-1,\"privateMsgThreshold\":0,\"privateMsgEnable\":false}", requestHeader, StandardCharsets.UTF_8);
        JSONObject rtmpObj = JSON.parseObject(rtmpJSON);
        String rtmpUrl = rtmpObj.getString("rtmpURL");
        if (StringUtils.isNotEmpty(rtmpUrl)) {
            return rtmpUrl;
        } else {
            accountInfo.setDisable(true);
            throw new RuntimeException("开启17Live直播间失败" + rtmpJSON);
        }
    }

    @Override
    public void setBroadcastSetting(AccountInfo accountInfo, String title, Integer areaId) {
        log.warn("setBroadcastSetting():暂不支持17Live的直播间设定");
    }

    @Override
    public String getBroadcastRoomId(AccountInfo accountInfo) throws Exception {
        if (StringUtils.isEmpty(accountInfo.getRoomId())) {
            Map<String, String> requestHeader = buildRequestHeader(accountInfo);
            String selfJSON = HttpRequestUtil.downloadUrl(new URI(API_17APP_GATEWAY), null, "data={\"action\":\"getSelfInfo\"}", requestHeader, StandardCharsets.UTF_8);
            JSONObject selfObj = JSON.parseObject(selfJSON).getJSONObject("data");
            if (selfObj != null) {
                accountInfo.setAccountId(selfObj.getString("openID"));
                accountInfo.setRoomId(selfObj.getString("roomID"));
                accountInfo.setNickname(selfObj.getString("openID"));
                accountInfo.setUid(selfObj.getString("userID"));
            } else {
                throw new RuntimeException("获取17Live直播间信息失败" + selfJSON);
            }
        }
        return accountInfo.getRoomId();
    }

    @Override
    public void stopBroadcast(AccountInfo accountInfo, boolean stopOnPadding) {
        log.warn("stopBroadcast():暂不支持17Live的直播间设定");
    }

    @Override
    public String getBroadcastCookies(String username, String password, String captcha) throws Exception {
        Map<String, String> requestHeader = buildRequestHeader(null);
        String loginJSON = HttpRequestUtil.downloadUrl(new URI(API_17WINWINSUN_GATEWAY), null, "data={\"openID\":\"" + username + "\",\"password\":\"" + DigestUtils.md5Hex(password) + "\",\"action\":\"loginAction\"}", requestHeader, StandardCharsets.UTF_8);
        JSONObject loginObj = JSONObject.parseObject(loginJSON).getJSONObject("data");
        if ("success".equals(loginObj.get("result"))) {
            return loginObj.getString("accessToken");
        } else {
            log.error("17Live登录失败" + loginJSON);
            String message = loginObj.getString("message");
            if ("no_such_user".equals(message)) {
                throw new RuntimeException("用户名或密码错误");
            } else {
                throw new RuntimeException("未知错误[" + message + "]");
            }
        }
    }

    @Override
    public InputStream getBroadcastCaptcha() throws IOException {
        return null;
    }

    private Map<String, String> buildRequestHeader(AccountInfo accountInfo) {
        Map<String, String> headerMap = new HashMap<>();
        if (accountInfo != null) {
            headerMap.put("accessToken", accountInfo.getCookies());
            if (accountInfo.getRoomId() != null) {
                headerMap.put("deviceID", DigestUtils.md5Hex(accountInfo.getRoomId()).substring(16));
            }
        }
        headerMap.put("packageName", "com.machipopo.media17");
        headerMap.put("version", "2.3.86.0");
        headerMap.put("language", "JP");
        headerMap.put("deviceType", "ANDROID");
        headerMap.put("deviceName", "SAMSUNG_SM-G9730_Android-8.0.0");
        headerMap.put("OSVersion", "26");
        headerMap.put("hardware", "SM-G9730");
        headerMap.put("debug-level", "0");
        headerMap.put("User-Agent", "okhttp/3.9.1");
        return headerMap;
    }
}
