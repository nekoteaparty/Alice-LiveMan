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
package site.alice.liveman.service.live.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.service.live.LiveService;
import site.alice.liveman.utils.HttpRequestUtil;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RealityLiveService extends LiveService {
    @Autowired
    private LiveManSetting          liveManSetting;
    private Map<String, JSONObject> streamerUsersMap = new ConcurrentHashMap<>(50);

    private void refreshStreamUsers() throws IOException, URISyntaxException {
        URI officialUsersUrl = new URI("https://user-prod-dot-vlive-prod.appspot.com/api/v1/streamer_users/list_streamer_official");
        JSONObject officialUsers = JSON.parseObject(HttpRequestUtil.downloadUrl(officialUsersUrl, null, "{\"official\":\"1\"}", StandardCharsets.UTF_8));
        JSONArray streamerUsers = officialUsers.getJSONObject("payload").getJSONArray("StreamerUsers");
        JSONObject unofficialUsers = JSON.parseObject(HttpRequestUtil.downloadUrl(officialUsersUrl, null, "{\"official\":\"2\"}", StandardCharsets.UTF_8));
        streamerUsers.addAll(unofficialUsers.getJSONObject("payload").getJSONArray("StreamerUsers"));
        for (int i = 0; i < streamerUsers.size(); i++) {
            JSONObject streamerUser = streamerUsers.getJSONObject(i);
            streamerUsersMap.put(streamerUser.getString("nickname"), streamerUser);
        }
    }

    @Override
    public URI getLiveVideoInfoUrl(ChannelInfo channelInfo) throws Exception {
        return new URI(channelInfo.getChannelUrl());
    }

    @Override
    public VideoInfo getLiveVideoInfo(URI videoInfoUrl, ChannelInfo channelInfo) throws Exception {
        if (videoInfoUrl == null) {
            return null;
        }
        String nickname = videoInfoUrl.toString().replace("reality://", "");
        JSONObject streamUser = streamerUsersMap.get(nickname);
        if (streamUser == null) {
            refreshStreamUsers();
            streamUser = streamerUsersMap.get(nickname);
        }
        if (streamUser == null) {
            log.warn(nickname + "的用户信息不存在，请核对！");
            return null;
        }
        String liveDetailJson = HttpRequestUtil.downloadUrl(new URI("https://media-prod-dot-vlive-prod.appspot.com/api/v1/media/get_from_vlid"), null, "{\"state\":30,\"vlive_id\":\"" + streamUser.getString("vlive_id") + "\"}", StandardCharsets.UTF_8);
        JSONObject liveDetailObj = JSON.parseObject(liveDetailJson);
        JSONArray lives = liveDetailObj.getJSONArray("payload");
        if (!lives.isEmpty()) {
            JSONObject liveObj = lives.getJSONObject(0);
            String videoId = liveObj.getString("media_id");
            String videoTitle = liveObj.getString("title");
            URI m3u8ListUrl = new URI(liveObj.getJSONObject("StreamingServer").getString("view_endpoint"));
            String[] m3u8List = HttpRequestUtil.downloadUrl(m3u8ListUrl, StandardCharsets.UTF_8).split("\n");
            String mediaUrl = null;
            for (int i = 0; i < m3u8List.length; i++) {
                if (m3u8List[i].contains(liveManSetting.getDefaultResolution())) {
                    mediaUrl = m3u8List[i + 1];
                    break;
                }
            }
            if (mediaUrl == null) {
                mediaUrl = m3u8List[3];
            }
            return new VideoInfo(channelInfo, videoId, videoTitle, m3u8ListUrl.resolve(mediaUrl), "m3u8");
        }
        return null;
    }

    @Override
    protected boolean isMatch(URI channelUrl) {
        return channelUrl.getScheme().contains("reality");
    }
}
