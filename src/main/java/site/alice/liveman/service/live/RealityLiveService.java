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
package site.alice.liveman.service.live;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.utils.HttpRequestUtil;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RealityLiveService extends LiveService {
    private static final Proxy                   WEB_PROXY        = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 8001));
    private              Map<String, JSONObject> streamerUsersMap = new ConcurrentHashMap<>(50);

    @Override
    protected VideoInfo getLiveVideoInfo(ChannelInfo channelInfo) throws Exception {
        String channelUrl = channelInfo.getChannelUrl();
        String nickname = channelUrl.replace("reality://", "");
        JSONObject streamUser = streamerUsersMap.get(nickname);
        if (streamUser == null) {
            refreshStreamUsers();
            streamUser = streamerUsersMap.get(nickname);
        }
        if (streamUser == null) {
            log.warn(nickname + "的用户信息不存在，请核对！");
            return null;
        }
        String liveDetailJson = HttpRequestUtil.downloadUrl(new URL("https://media-prod-dot-vlive-prod.appspot.com/api/v1/media/get_from_vlid"), null, "{\"state\":30,\"vlive_id\":\"" + streamUser.getString("vlive_id") + "\"}", StandardCharsets.UTF_8, WEB_PROXY);
        JSONObject liveDetailObj = JSON.parseObject(liveDetailJson);
        JSONArray lives = liveDetailObj.getJSONArray("payload");
        if (!lives.isEmpty()) {
            JSONObject liveObj = lives.getJSONObject(0);
            String videoId = liveObj.getString("media_id");
            String videoTitle = liveObj.getString("title");
            URL m3u8ListUrl = new URL(liveObj.getJSONObject("StreamingServer").getString("view_endpoint"));
            String[] m3u8List = HttpRequestUtil.downloadUrl(m3u8ListUrl, StandardCharsets.UTF_8, null).split("\n");
            String mediaUrl = null;
            for (int i = 0; i < m3u8List.length; i++) {
                if (m3u8List[i].contains("720x1280")) {
                    mediaUrl = m3u8List[i + 1];
                    break;
                }
            }
            if (mediaUrl == null) {
                mediaUrl = m3u8List[3];
            }
            return new VideoInfo(channelInfo, videoId, videoTitle, m3u8ListUrl.toURI().resolve(mediaUrl), "m3u8");
        }
        return null;
    }

    private void refreshStreamUsers() throws IOException {
        URL officialUsersUrl = new URL("https://user-prod-dot-vlive-prod.appspot.com/api/v1/streamer_users/list_streamer_official");
        JSONObject officialUsers = JSON.parseObject(HttpRequestUtil.downloadUrl(officialUsersUrl, null, "{\"official\":\"1\"}", StandardCharsets.UTF_8, WEB_PROXY));
        JSONArray streamerUsers = officialUsers.getJSONObject("payload").getJSONArray("StreamerUsers");
        JSONObject unofficialUsers = JSON.parseObject(HttpRequestUtil.downloadUrl(officialUsersUrl, null, "{\"official\":\"2\"}", StandardCharsets.UTF_8, WEB_PROXY));
        streamerUsers.addAll(unofficialUsers.getJSONObject("payload").getJSONArray("StreamerUsers"));
        for (int i = 0; i < streamerUsers.size(); i++) {
            JSONObject streamerUser = streamerUsers.getJSONObject(i);
            streamerUsersMap.put(streamerUser.getString("nickname"), streamerUser);
        }
    }

    @Override
    protected boolean isMatch(URI channelUrl) {
        return channelUrl.getScheme().contains("reality");
    }
}
