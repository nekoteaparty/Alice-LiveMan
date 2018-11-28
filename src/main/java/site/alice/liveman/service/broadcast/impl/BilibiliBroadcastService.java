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
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import site.alice.liveman.model.AccountInfo;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.service.broadcast.BroadcastService;
import site.alice.liveman.utils.HttpRequestUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class BilibiliBroadcastService implements BroadcastService {
    private static final String BILI_LIVE_UPDATE_URL = "https://api.live.bilibili.com/room/v1/Room/update";
    private static final String BILI_START_LIVE_URL  = "https://api.live.bilibili.com/room/v1/Room/startLive";
    private static final String BILI_LIVE_INFO_URL   = "https://api.live.bilibili.com/live_user/v1/UserInfo/live_info";

    @Override
    public boolean isMatch(String accountSite) {
        return "bilibili".equals(accountSite);
    }

    @Override
    public String getBroadcastAddress(AccountInfo accountInfo) throws Exception {
        VideoInfo videoInfo = accountInfo.getCurrentVideo();
        int area = 33;
        if (videoInfo.getArea() != null) {
            area = videoInfo.getArea()[1];
        }
        try {
            Matcher matcher = Pattern.compile("bili_jct=(.{32})").matcher(accountInfo.getCookies());
            String csrfToken = "";
            if (matcher.find()) {
                csrfToken = matcher.group(1);
            }
            String title = videoInfo.getTitle().length() > 20 ? videoInfo.getTitle().substring(0, 20) : videoInfo.getTitle();
            String postData = "room_id=" + getBroadcastRoomId(accountInfo) + "&title=" + title + "&area_id=" + area + "&csrf_token=" + csrfToken;
            String resJson = HttpRequestUtil.downloadUrl(new URI(BILI_LIVE_UPDATE_URL), accountInfo.getCookies(), postData, StandardCharsets.UTF_8);
            JSONObject resObject = JSON.parseObject(resJson);
            if (resObject.getInteger("code") != 0) {
                log.error("修改直播间信息失败[title=" + title + ", area_id=" + area + "]" + resJson);
            }
        } catch (Throwable e) {
            log.error("修改直播间标题为[" + videoInfo.getTitle() + "]失败", e);
        }
        String startLiveJson = HttpRequestUtil.downloadUrl(new URI(BILI_START_LIVE_URL), accountInfo.getCookies(), "room_id=" + accountInfo.getRoomId() + "&platform=pc&area_v2=" + area, StandardCharsets.UTF_8);
        JSONObject startLiveObject = JSON.parseObject(startLiveJson);
        JSONObject rtmpObject;
        if (startLiveObject.get("data") instanceof JSONObject) {
            rtmpObject = startLiveObject.getJSONObject("data").getJSONObject("rtmp");
        } else {
            accountInfo.setDisable(true);
            throw new RuntimeException("开启B站直播间失败" + startLiveJson);
        }
        return rtmpObject.getString("addr") + rtmpObject.getString("code");
    }

    @Override
    public String getBroadcastRoomId(AccountInfo accountInfo) throws Exception {
        if (StringUtils.isEmpty(accountInfo.getRoomId())) {
            String liveInfoJson = HttpRequestUtil.downloadUrl(new URI(BILI_LIVE_INFO_URL), accountInfo.getCookies(), Collections.emptyMap(), StandardCharsets.UTF_8);
            JSONObject liveInfoObject = JSON.parseObject(liveInfoJson);
            if (liveInfoObject.get("data") instanceof JSONObject) {
                JSONObject data = liveInfoObject.getJSONObject("data");
                accountInfo.setRoomId(data.getString("roomid"));
                accountInfo.setNickname(data.getJSONObject("userInfo").getString("uname"));
                accountInfo.setAccountId(accountInfo.getNickname());
            } else {
                throw new RuntimeException("获取B站直播间信息失败" + liveInfoObject);
            }
        }
        return accountInfo.getRoomId();
    }
}
