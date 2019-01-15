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
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.service.live.LiveService;
import site.alice.liveman.utils.HttpRequestUtil;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PscpLiveService extends LiveService {

    private static final Pattern dataStorePattern     = Pattern.compile("data-store=\"(.+?)\">");
    private static final String  LIVE_VIDEO_URL       = "https://www.pscp.tv/w/";
    private static final String  accessVideoPublicUrl = "https://proxsee.pscp.tv/api/v2/accessVideoPublic?broadcast_id=";

    @Override
    public URI getLiveVideoInfoUrl(ChannelInfo channelInfo) throws Exception {
        String channelUrl = channelInfo.getChannelUrl();
        String channelHtml = HttpRequestUtil.downloadUrl(new URI(channelUrl), channelInfo.getChannelUrl(), Collections.emptyMap(), StandardCharsets.UTF_8);
        Matcher dataStoreMatcher = dataStorePattern.matcher(channelHtml);
        if (dataStoreMatcher.find()) {
            String dataStore = StringEscapeUtils.unescapeJava(dataStoreMatcher.group(0));
            JSONObject dataStoreObj = JSON.parseObject(dataStore);
            JSONObject broadcasts = dataStoreObj.getJSONObject("BroadcastCache").getJSONObject("broadcasts");
            for (String videoId : broadcasts.keySet()) {
                JSONObject broadcastObj = broadcasts.getJSONObject(videoId);
                String state = broadcastObj.getJSONObject("broadcast").getString("state");
                if ("RUNNING".equals(state)) {
                    return new URI(LIVE_VIDEO_URL + videoId);
                }
            }
        } else {
            throw new RuntimeException("没有找到DataStore[" + channelUrl + "]");
        }
        return null;
    }

    @Override
    public VideoInfo getLiveVideoInfo(URI videoInfoUrl, ChannelInfo channelInfo) throws Exception {
        String broadcastId = videoInfoUrl.getPath().replace("/w/", "");
        String json = HttpRequestUtil.downloadUrl(new URI(accessVideoPublicUrl + broadcastId), channelInfo.getCookies(), Collections.emptyMap(), StandardCharsets.UTF_8);
        JSONObject accessVideoPublic = JSON.parseObject(json);
        JSONObject broadcast = accessVideoPublic.getJSONObject("broadcast");
        String title = broadcast.getString("status");
        String hlsUrl = accessVideoPublic.getString("hls_url");
        if (StringUtils.isNotEmpty(hlsUrl)) {
            return new VideoInfo(channelInfo, broadcastId, title, new URI(hlsUrl), "m3u8");
        }
        return null;
    }

    @Override
    protected boolean isMatch(URI channelUrl) {
        return channelUrl.getHost().contains("pscp.tv");
    }
}
