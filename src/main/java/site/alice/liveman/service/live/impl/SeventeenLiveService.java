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
import org.springframework.stereotype.Service;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.service.live.LiveService;
import site.alice.liveman.utils.HttpRequestUtil;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@Service
public class SeventeenLiveService extends LiveService {

    private static final String liveStreamInfoUrl = "https://api-dsa.17app.co/api/v1/liveStreams/getLiveStreamInfo";

    @Override
    public URI getLiveVideoInfoUrl(ChannelInfo channelInfo) throws Exception {
        return new URI(channelInfo.getChannelUrl());
    }

    @Override
    public VideoInfo getLiveVideoInfo(URI videoInfoUrl, ChannelInfo channelInfo, String resolution) throws Exception {
        String[] pathSplit = videoInfoUrl.getPath().split("/");
        String profileId = pathSplit[pathSplit.length - 1];
        String liveStreamInfo = HttpRequestUtil.downloadUrl(new URI(liveStreamInfoUrl), channelInfo == null ? null : channelInfo.getCookies(), "{\"liveStreamID\":\"" + profileId + "\"}", StandardCharsets.UTF_8);
        JSONObject jsonObject = JSON.parseObject(liveStreamInfo).getJSONObject("data");
        JSONArray rtmpUrls = jsonObject.getJSONArray("rtmpUrls");
        if (jsonObject.getInteger("status") > 0 && rtmpUrls != null && rtmpUrls.size() > 0) {
            URI videoUrl = new URI(rtmpUrls.getJSONObject(0).getString("url"));
            return new VideoInfo(channelInfo, profileId, jsonObject.getString("caption"), videoInfoUrl, videoUrl, "flv");
        }
        return null;
    }

    @Override
    protected boolean isMatch(URI channelUrl) {
        return channelUrl.getHost().contains("17.live");
    }
}
