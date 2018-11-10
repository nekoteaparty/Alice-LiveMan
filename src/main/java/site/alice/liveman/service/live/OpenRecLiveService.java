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
import org.springframework.stereotype.Service;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.utils.HttpRequestUtil;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
public class OpenRecLiveService extends LiveService {
    @Override
    public VideoInfo getLiveVideoInfo(ChannelInfo channelInfo) throws Exception {
        String channelName = channelInfo.getChannelUrl().replace("https://www.openrec.tv/user/", "");
        URI moviesUrl = new URI("https://public.openrec.tv/external/api/v5/movies?channel_id=" + channelName + "&sort=onair_status");
        String moviesJson = HttpRequestUtil.downloadUrl(moviesUrl, StandardCharsets.UTF_8, null);
        JSONArray movies = JSON.parseArray(moviesJson);
        if (!movies.isEmpty()) {
            JSONObject movieObj = (JSONObject) movies.get(0);
            Integer onAirStatus = movieObj.getInteger("onair_status");
            if (onAirStatus == 1) {
                String videoId = movieObj.getString("id");
                String videoTitle = movieObj.getString("title");
                URI m3u8ListUrl = new URI(movieObj.getJSONObject("media").getString("url"));
                String[] m3u8List = HttpRequestUtil.downloadUrl(m3u8ListUrl, StandardCharsets.UTF_8, null).split("\n");
                String mediaUrl = null;
                for (int i = 0; i < m3u8List.length; i++) {
                    if (m3u8List[i].contains("1280x720")) {
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
        return channelUrl.getHost().contains("openrec.tv");
    }
}
