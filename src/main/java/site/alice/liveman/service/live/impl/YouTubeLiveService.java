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
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.service.live.LiveService;
import site.alice.liveman.utils.HttpRequestUtil;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class YouTubeLiveService extends LiveService {

    @Autowired
    private              LiveManSetting liveManSetting;
    private static final String         LIVE_VIDEO_SUFFIX   = "/videos?view=2&flow=grid";
    private static final String         GET_VIDEO_INFO_URL  = "https://www.youtube.com/watch?v=";
    private static final Pattern        initDataJsonPattern = Pattern.compile("window\\[\"ytInitialData\"] = (.+?);\n");
    private static final Pattern        hlsvpPattern        = Pattern.compile("\"hlsvp\":\"(.+?)\"");
    private static final Pattern        videoTitlePattern   = Pattern.compile("\"title\":\"(.+?)\"");
    private static final Pattern        videoIdPattern      = Pattern.compile("\"video_id\":\"(.+?)\"");
    private static final Pattern        browseIdPattern     = Pattern.compile("RICH_METADATA_RENDERER_STYLE_BOX_ART.+?\\{\"browseId\":\"(.+?)\"}");

    @Override
    public URI getLiveVideoInfoUrl(ChannelInfo channelInfo) throws Exception {
        String channelUrl = channelInfo.getChannelUrl();
        URI url = new URI(channelUrl + LIVE_VIDEO_SUFFIX);
        String resHtml = HttpRequestUtil.downloadUrl(url, StandardCharsets.UTF_8);
        Matcher matcher = initDataJsonPattern.matcher(resHtml);
        if (matcher.find()) {
            String initDataJson = matcher.group(1);
            JSONObject jsonObject = JSON.parseObject(initDataJson);
            JSONArray tabs = jsonObject.getJSONObject("contents").getJSONObject("twoColumnBrowseResultsRenderer").getJSONArray("tabs");
            JSONArray gridVideoRender = null;
            for (Object tab : tabs) {
                JSONObject tabObject = (JSONObject) tab;
                JSONObject tabRenderer = tabObject.getJSONObject("tabRenderer");
                if (tabRenderer != null && tabRenderer.getBoolean("selected")) {
                    gridVideoRender = tabRenderer.getJSONObject("content").getJSONObject("sectionListRenderer").getJSONArray("contents").getJSONObject(0).getJSONObject("itemSectionRenderer").getJSONArray("contents").getJSONObject(0).getJSONObject("gridRenderer").getJSONArray("items");
                    break;
                }
            }
            String videoId = null;
            if (gridVideoRender != null) {
                for (Object reader : gridVideoRender) {
                    JSONObject readerObject = (JSONObject) reader;
                    if (readerObject.toJSONString().contains("BADGE_STYLE_TYPE_LIVE_NOW")) {
                        videoId = readerObject.getJSONObject("gridVideoRenderer").getString("videoId");
                        return new URI(GET_VIDEO_INFO_URL + videoId);
                    }
                }
            }
        } else {
            throw new RuntimeException("没有找到InitData[" + url + "]");
        }
        return null;
    }

    @Override
    public VideoInfo getLiveVideoInfo(URI videoInfoUrl, ChannelInfo channelInfo) throws Exception {
        if (videoInfoUrl == null) {
            return null;
        }
        String videoInfoRes = HttpRequestUtil.downloadUrl(videoInfoUrl, StandardCharsets.UTF_8);
        Matcher videoIdMatcher = videoIdPattern.matcher(videoInfoRes);
        Matcher hlsvpMatcher = hlsvpPattern.matcher(videoInfoRes);
        Matcher videoTitleMatcher = videoTitlePattern.matcher(videoInfoRes);
        Matcher browseIdMatcher = browseIdPattern.matcher(videoInfoRes);
        String videoTitle = "";
        String description = "";
        String videoId = "";
        if (videoIdMatcher.find()) {
            videoId = videoIdMatcher.group(1);
        }
        if (StringUtils.isEmpty(videoId)) {
            throw new RuntimeException("获取视频VideoId失败！");
        }
        if (videoTitleMatcher.find()) {
            videoTitle = videoTitleMatcher.group(1);
        }
        if (browseIdMatcher.find()) {
            description = browseIdMatcher.group(1);
        }
        if (hlsvpMatcher.find()) {
            String hlsvpUrl = URLDecoder.decode(StringEscapeUtils.unescapeJava(hlsvpMatcher.group(1)), StandardCharsets.UTF_8.name());
            String[] m3u8List = HttpRequestUtil.downloadUrl(new URI(hlsvpUrl), StandardCharsets.UTF_8).split("\n");
            String mediaUrl = null;
            for (int i = 0; i < m3u8List.length; i++) {
                if (m3u8List[i].contains(liveManSetting.getDefaultResolution())) {
                    mediaUrl = m3u8List[i + 1];
                    // 这里不需要加break，取相同分辨率下码率最高的
                }
            }
            if (mediaUrl == null) {
                mediaUrl = m3u8List[m3u8List.length - 1];
            }
            VideoInfo videoInfo = new VideoInfo(channelInfo, videoId, videoTitle, new URI(mediaUrl), "m3u8");
            videoInfo.setDescription(description);
            return videoInfo;
        } else if (videoInfoRes.contains("LIVE_STREAM_OFFLINE")) {
            return null;
        } else {
            throw new RuntimeException("没有找到InitData[" + GET_VIDEO_INFO_URL + videoId + "]");
        }
    }

    @Override
    public boolean isMatch(URI channelUrl) {
        return channelUrl.getHost().contains("youtube.com");
    }
}
