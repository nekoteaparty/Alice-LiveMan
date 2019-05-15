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
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.service.live.LiveService;
import site.alice.liveman.utils.HttpRequestUtil;
import site.alice.liveman.utils.M3u8Util;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class ColonLiveService extends LiveService {

    public static final String USER_PROFILE_API   = "https://api.colon-live.com/App/User/Profile/%s";
    public static final String LIVE_SHOW_API      = "https://api.colon-live.com/v3/App/Show/%s?devicePlatform=iOS";
    public static final String VTUBER_PROFILE_URL = "https://colon-live.com/Usr/VTuberProfile?vTuberUserId=";
    public static final String LIVE_SHOW_URL      = "https://colon-live.com/Shows/Live/";


    @Override
    public URI getLiveVideoInfoUrl(ChannelInfo channelInfo) throws Exception {
        String channelUrl = channelInfo.getChannelUrl();
        if (channelUrl.startsWith(VTUBER_PROFILE_URL)) {
            String uuid = channelUrl.replace(VTUBER_PROFILE_URL, "");
            String profileJson = HttpRequestUtil.downloadUrl(new URI(String.format(USER_PROFILE_API, uuid)), StandardCharsets.UTF_8);
            JSONObject profileData = JSON.parseObject(profileJson);
            JSONArray jsonArray = profileData.getJSONObject("liveShowInfos").getJSONArray("results");
            if (!jsonArray.isEmpty()) {
                return new URI(LIVE_SHOW_URL + jsonArray.getJSONObject(0).getString("showId"));
            }
        }
        return null;
    }

    @Override
    public VideoInfo getLiveVideoInfo(URI videoInfoUrl, ChannelInfo channelInfo, String resolution) throws Exception {
        if (videoInfoUrl == null) {
            return null;
        }
        if (!videoInfoUrl.toString().startsWith(LIVE_SHOW_URL)) {
            return null;
        }
        String showId = videoInfoUrl.toString().replace(LIVE_SHOW_URL, "");
        String liveShowJson = HttpRequestUtil.downloadUrl(new URI(String.format(LIVE_SHOW_API, showId)), StandardCharsets.UTF_8);
        JSONObject liveShowData = JSON.parseObject(liveShowJson);
        String videoId = liveShowData.getString("showId");
        String videoTitle = liveShowData.getString("name");
        String masterM3u8URL = liveShowData.getString("liveStreamUrl");
        if (masterM3u8URL == null || !masterM3u8URL.contains("playlist.m3u8")) {
            return null;
        }
        URI m3u8ListUri = new URI(masterM3u8URL);
        String masterM3u8 = HttpRequestUtil.downloadUrl(m3u8ListUri, StandardCharsets.UTF_8);
        String[] m3u8Lines = masterM3u8.split("\n");
        String m3u8FileUrl = null;
        boolean isFindResolution = false;
        M3u8Util.StreamInfo streamInfo = null;
        for (String m3u8Line : m3u8Lines) {
            if (StringUtils.isNotEmpty(m3u8Line)) {
                if (m3u8Line.startsWith("#")) {
                    if (m3u8Line.contains(resolution)) {
                        isFindResolution = true;
                    }
                    if (m3u8Line.startsWith("#EXT-X-STREAM-INF")) {
                        if (streamInfo == null || isFindResolution) {
                            streamInfo = M3u8Util.getStreamInfo(m3u8Line);
                        }
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
        VideoInfo videoInfo = new VideoInfo(channelInfo, videoId, videoTitle, videoInfoUrl, m3u8ListUri.resolve(m3u8FileUrl), "m3u8");
        if (streamInfo != null) {
            videoInfo.setResolution(streamInfo.getResolution());
            videoInfo.setFrameRate(streamInfo.getFrameRate());
        }
        return videoInfo;
    }

    @Override
    protected boolean isMatch(URI channelUrl) {
        return channelUrl.getHost().contains("colon-live.com");
    }
}
