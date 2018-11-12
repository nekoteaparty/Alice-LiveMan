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
package site.alice.liveman.service.live.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.service.live.LiveService;
import site.alice.liveman.utils.HttpRequestUtil;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
public class MirrativLiveService extends LiveService {

    @Override
    public VideoInfo getLiveVideoInfo(ChannelInfo channelInfo) throws Exception {
        String channelUrl = channelInfo.getChannelUrl();
        String userId = channelUrl.replace("https://www.mirrativ.com/user/", "").replace("/", "");
        URI liveHistoryUrl = new URI("https://www.mirrativ.com/api/live/live_history?user_id=" + userId + "&page=1");
        String liveHistoryJson = HttpRequestUtil.downloadUrl(liveHistoryUrl, StandardCharsets.UTF_8);
        JSONObject liveHistory = JSON.parseObject(liveHistoryJson);
        JSONArray lives = liveHistory.getJSONArray("lives");
        if (!lives.isEmpty()) {
            JSONObject liveObj = lives.getJSONObject(0);
            if (liveObj.getBoolean("is_live")) {
                String videoId = liveObj.getString("live_id");
                String liveDetailJson = HttpRequestUtil.downloadUrl(new URI("https://www.mirrativ.com/api/live/live?live_id=" + videoId), StandardCharsets.UTF_8);
                JSONObject liveDetailObj = JSON.parseObject(liveDetailJson);
                String videoTitle = liveDetailObj.getString("title");
                URI m3u8ListUrl = new URI(liveDetailObj.getString("streaming_url_hls"));
                String[] m3u8List = HttpRequestUtil.downloadUrl(m3u8ListUrl, StandardCharsets.UTF_8).split("\n");
                String mediaUrl = null;
                for (int i = 0; i < m3u8List.length; i++) {
                    if (m3u8List[i].contains("432x768")) {
                        mediaUrl = m3u8List[i + 1];
                        break;
                    }
                }
                if (mediaUrl == null) {
                    mediaUrl = m3u8List[3];
                }
                return new VideoInfo(channelInfo, videoId, videoTitle, m3u8ListUrl.resolve(mediaUrl), "m3u8");
            }
        }
        return null;
    }

    @Override
    public boolean isMatch(URI channelUrl) {
        return channelUrl.getHost().contains("mirrativ.com");
    }
}
