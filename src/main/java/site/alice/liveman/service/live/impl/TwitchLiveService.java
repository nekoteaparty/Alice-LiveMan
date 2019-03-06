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
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.service.live.LiveService;
import site.alice.liveman.utils.HttpRequestUtil;
import site.alice.liveman.utils.M3u8Util;
import site.alice.liveman.utils.M3u8Util.StreamInfo;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class TwitchLiveService extends LiveService {

    private static final String clientId             = "jzkbprff40iqj646a697cyrvl0zt2m6";
    private static final String GET_STREAM_INFO_URL  = "https://api.twitch.tv/kraken/streams/";
    private static final String GET_STREAM_TOKEN_URL = "https://api.twitch.tv/api/channels/%s/access_token?need_https=true&oauth_token&platform=web&player_backend=mediaplayer&player_type=site";
    private static final String MASTER_M3U8_URL      = "https://usher.ttvnw.net/api/channel/hls/%s.m3u8?allow_source=true&playlist_include_framerate=true&sig=%s&token=%s";

    @Autowired
    private LiveManSetting liveManSetting;

    @Override
    public VideoInfo getLiveVideoInfo(URI videoInfoUrl, ChannelInfo channelInfo, String resolution) throws Exception {
        if (videoInfoUrl == null) {
            return null;
        }
        String channelUrl = videoInfoUrl.toString();
        String channelName = channelUrl.substring(22);
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("client-id", clientId);
        String streamJSON = HttpRequestUtil.downloadUrl(new URI(GET_STREAM_INFO_URL + channelName), channelInfo != null ? channelInfo.getCookies() : null, headerMap, StandardCharsets.UTF_8);
        JSONObject streamObj = JSON.parseObject(streamJSON).getJSONObject("stream");
        if (streamObj != null) {
            String videoId = streamObj.getString("_id");
            JSONObject channelObj = streamObj.getJSONObject("channel");
            String videoTitle = channelObj.getString("status");
            String streamTokenJSON = HttpRequestUtil.downloadUrl(new URI(String.format(GET_STREAM_TOKEN_URL, channelName)), channelInfo != null ? channelInfo.getCookies() : null, headerMap, StandardCharsets.UTF_8);
            JSONObject streamTokenObj = JSON.parseObject(streamTokenJSON);
            String token = streamTokenObj.getString("token");
            String sig = streamTokenObj.getString("sig");
            String masterM3u8URL = String.format(MASTER_M3U8_URL, channelName, sig, URLEncoder.encode(token, StandardCharsets.UTF_8.name()));
            String masterM3u8 = HttpRequestUtil.downloadUrl(new URI(masterM3u8URL), null, headerMap, StandardCharsets.UTF_8);
            String[] m3u8Lines = masterM3u8.split("\n");
            String m3u8FileUrl = null;
            boolean isFindResolution = false;
            StreamInfo streamInfo = null;
            for (String m3u8Line : m3u8Lines) {
                if (StringUtils.isNotEmpty(m3u8Line)) {
                    if (m3u8Line.startsWith("#")) {
                        if (m3u8Line.startsWith("#EXT-X-STREAM-INF")) {
                            streamInfo = M3u8Util.getStreamInfo(m3u8Line);
                        }
                        if (m3u8Line.contains(resolution)) {
                            isFindResolution = true;
                        }
                    } else {
                        // 默认优先第一个最高码流的
                        if (m3u8FileUrl == null) {
                            m3u8FileUrl = m3u8Line;
                        }
                        // 如果找到了指定的码率则覆盖默认的码流地址
                        if (isFindResolution) {
                            m3u8FileUrl = m3u8Line;
                            break;
                        }
                    }
                }
            }
            VideoInfo videoInfo = new VideoInfo(channelInfo, videoId, videoTitle, videoInfoUrl, new URI(m3u8FileUrl), "m3u8");
            if (streamInfo != null) {
                videoInfo.setResolution(streamInfo.getResolution());
                videoInfo.setFrameRate(streamInfo.getFrameRate());
            }
            videoInfo.setDescription(streamObj.getString("game"));
            return videoInfo;
        }
        return null;
    }

    @Override
    public URI getLiveVideoInfoUrl(ChannelInfo channelInfo) throws Exception {
        String channelUrl = channelInfo.getChannelUrl();
        String channelName = channelUrl.substring(22);
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("client-id", clientId);
        String streamJSON = HttpRequestUtil.downloadUrl(new URI(GET_STREAM_INFO_URL + channelName), channelInfo != null ? channelInfo.getCookies() : null, headerMap, StandardCharsets.UTF_8);
        JSONObject streamObj = JSON.parseObject(streamJSON).getJSONObject("stream");
        if (streamObj != null && "live".equals(streamObj.getString("stream_type"))) {
            return new URI(channelUrl);
        }
        return null;
    }

    @Override
    protected boolean isMatch(URI channelUrl) {
        return channelUrl.getHost().contains("twitch.tv");
    }
}
