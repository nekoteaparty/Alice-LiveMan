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
public class NicoLiveService extends LiveService {

    private static final Pattern LIVE_VIDEO_URL_PATTERN = Pattern.compile("<p class=\"live_btn\"><a href=\"(.+?)\">");
    private static final Pattern EMBEDDED_DATA_PATTERN  = Pattern.compile("<script id=\"embedded-data\" data-props=\"(.+?)\">");
    private static final Pattern WEB_SOCKET_URL_PATTERN = Pattern.compile("&quot;(wss://.+?)&quot;");
    private static final Pattern TITLE_PATTERN          = Pattern.compile("&quot;title&quot;:&quot;(.+?)&quot;");
    private static final Pattern VIDEO_ID_PATTERN       = Pattern.compile("&quot;nicoliveProgramId&quot;:&quot;(.+?)&quot;");

    @Override
    public URI getLiveVideoInfoUrl(ChannelInfo channelInfo) throws Exception {
        String channelUrl = channelInfo.getChannelUrl();
        if (!channelUrl.endsWith("/live")) {
            channelUrl += "/live";
        }
        String html = HttpRequestUtil.downloadUrl(new URI(channelUrl), channelInfo != null ? channelInfo.getCookies() : null, Collections.emptyMap(), StandardCharsets.UTF_8);
        Matcher matcher = LIVE_VIDEO_URL_PATTERN.matcher(html);
        if (matcher.find()) {
            return new URI(matcher.group(1));
        }
        return null;
    }

    @Override
    public VideoInfo getLiveVideoInfo(URI videoInfoUrl, ChannelInfo channelInfo, String resolution) throws Exception {
        if (videoInfoUrl == null) {
            return null;
        }
        String html = HttpRequestUtil.downloadUrl(videoInfoUrl, channelInfo != null ? channelInfo.getCookies() : null, Collections.emptyMap(), StandardCharsets.UTF_8);
        Matcher matcher = EMBEDDED_DATA_PATTERN.matcher(html);
        if (matcher.find()) {
            String embeddedData = matcher.group(1);
            String webSocketUrl = "";
            String videoTitle = "";
            String videoId = "";
            Matcher webSocketUrlMatcher = WEB_SOCKET_URL_PATTERN.matcher(embeddedData);
            if (webSocketUrlMatcher.find()) {
                webSocketUrl = webSocketUrlMatcher.group(1);
            }
            Matcher titleMatcher = TITLE_PATTERN.matcher(embeddedData);
            if (titleMatcher.find()) {
                videoTitle = titleMatcher.group(1);
            }
            Matcher videoIdMatcher = VIDEO_ID_PATTERN.matcher(embeddedData);
            if (videoIdMatcher.find()) {
                videoId = videoIdMatcher.group(1);
            }
            return new VideoInfo(channelInfo, videoId, videoTitle, videoInfoUrl, new URI(webSocketUrl), "m3u8");
        } else {
            throw new RuntimeException("没有找到EmbeddedData[" + videoInfoUrl + "]");
        }
    }

    @Override
    protected boolean isMatch(URI channelUrl) {
        return channelUrl.getHost().contains("nicovideo.jp");
    }
}
