/*
 * <Alice LiveMan>
 * Copyright (C) <2018>  <NekoSunflower>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package site.alice.liveman.service.broadcast.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import site.alice.liveman.model.AccountInfo;
import site.alice.liveman.service.broadcast.BroadcastService;
import site.alice.liveman.utils.HttpRequestUtil;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Service
public class BilibiliBroadcastService implements BroadcastService {
    private static final String BILI_START_LIVE_URL = "https://api.live.bilibili.com/room/v1/Room/startLive";
    private static final String BILI_LIVE_INFO_URL  = "https://api.live.bilibili.com/live_user/v1/UserInfo/live_info";

    @Override
    public boolean isMatch(String accountSite) {
        return "bilibili".equals(accountSite);
    }

    @Override
    public String getBroadcastAddress(AccountInfo accountInfo) throws Exception {
        if (StringUtils.isEmpty(accountInfo.getRoomId())) {
            String liveInfoJson = HttpRequestUtil.downloadUrl(new URI(BILI_LIVE_INFO_URL), accountInfo.getCookies(), Collections.emptyMap(), StandardCharsets.UTF_8, null);
            JSONObject liveInfoObject = JSON.parseObject(liveInfoJson);
            if (liveInfoObject.get("data") instanceof JSONObject) {
                accountInfo.setRoomId(liveInfoObject.getJSONObject("data").getString("roomid"));
            } else {
                throw new Exception("开启B站直播间失败" + liveInfoObject);
            }
        }
        String startLiveJson = HttpRequestUtil.downloadUrl(new URI(BILI_START_LIVE_URL), accountInfo.getCookies(), "room_id=" + accountInfo.getRoomId() + "&platform=pc&area_v2=33", StandardCharsets.UTF_8, null);
        JSONObject startLiveObject = JSON.parseObject(startLiveJson);
        JSONObject rtmpObject;
        if (startLiveObject.get("data") instanceof JSONObject) {
            rtmpObject = startLiveObject.getJSONObject("data").getJSONObject("rtmp");
        } else {
            throw new Exception("开启B站直播间失败" + startLiveJson);
        }
        return rtmpObject.getString("addr") + rtmpObject.getString("code");
    }
}
