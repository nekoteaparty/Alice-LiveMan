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

import com.alibaba.fastjson.JSONObject;
import com.sun.org.apache.xpath.internal.operations.Bool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.service.live.LiveService;
import site.alice.liveman.utils.HttpRequestUtil;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class TwitcastingLiveService extends LiveService {

    private static final Pattern ROOM_TITLE_PATTERN = Pattern.compile("<meta name=\"twitter:title\" content=\"(.+?)\">");

    @Override
    public URI getLiveVideoInfoUrl(ChannelInfo channelInfo) throws Exception {
        return new URI(channelInfo.getChannelUrl());
    }

    @Override
    public VideoInfo getLiveVideoInfo(URI videoInfoUrl, ChannelInfo channelInfo) throws Exception {
        if (videoInfoUrl == null) {
            return null;
        }
        String roomName = videoInfoUrl.toString().replace("https://twitcasting.tv/", "").replace("/", "");
        URI streamCheckerUrl = new URI("https://twitcasting.tv/streamchecker.php?u=" + roomName + "&v=999&myself=&islive=1&lastitemid=-1&__c=" + System.currentTimeMillis());
        String streamChecker = HttpRequestUtil.downloadUrl(streamCheckerUrl, StandardCharsets.UTF_8);
        String[] checkes = streamChecker.split("\t");
        Video video = parseVideo(checkes[0], Integer.parseInt(checkes[1]), checkes[7], Integer.parseInt(checkes[19].trim()));
        if (!video.getOnline()) {
            return null;
        }
        if (video.getPrivate()) {
            log.warn("频道[" + channelInfo.getChannelName() + "]正在直播的内容已加密，无法转播！");
        }
        if (video.getWatchable()) {
            URI streamServerUrl = new URI("https://twitcasting.tv/streamserver.php?target=" + roomName + "&mode=client");
            String serverInfo = HttpRequestUtil.downloadUrl(streamServerUrl, StandardCharsets.UTF_8);

            JSONObject streamServer = JSONObject.parseObject(serverInfo);
            JSONObject movie = streamServer.getJSONObject("movie");
            if (movie.getBoolean("live")) {
                String videoTitle = "";
                String roomHtml = HttpRequestUtil.downloadUrl(videoInfoUrl, StandardCharsets.UTF_8);
                Matcher matcher = ROOM_TITLE_PATTERN.matcher(roomHtml);
                if (matcher.find()) {
                    videoTitle = matcher.group(1);
                }
                String videoId = movie.getString("id");
                String mediaUrl = "wss://" + streamServer.getJSONObject("fmp4").getString("host") + "/ws.app/stream/" + videoId + "/fmp4/bd/1/1500?mode=main";
                return new VideoInfo(channelInfo, videoId, video.getTelop() == null ? videoTitle : video.getTelop(), new URI(mediaUrl), "mp4");
            }
        }
        return null;
    }

    /**
     * Watchable = 0, Private = 5, Offline = 7
     *
     * @return
     */
    private Video parseVideo(String id, int watchable, String telop, int status) throws UnsupportedEncodingException {
        boolean isNeverShowState = (status & 1) != 0, isPrivate = (status & 2) != 0;
        Video video = new Video();
        video.setId(id.trim());
        video.setOnline(watchable != 7);
        video.setWatchable(watchable == 0);
        video.setNeverShowState(isNeverShowState);
        video.setPrivate(isPrivate);
        video.setTelop(StringUtils.isBlank(telop) ? null : URLDecoder.decode(telop, StandardCharsets.UTF_8.name()));
        return video;
    }

    private class Video {
        private String  id;
        private Boolean isOnline;
        private Boolean isWatchable;
        private Boolean isNeverShowState;
        private Boolean isPrivate;
        private String  telop;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Boolean getOnline() {
            return isOnline;
        }

        public void setOnline(Boolean online) {
            isOnline = online;
        }

        public Boolean getWatchable() {
            return isWatchable;
        }

        public void setWatchable(Boolean watchable) {
            isWatchable = watchable;
        }

        public Boolean getNeverShowState() {
            return isNeverShowState;
        }

        public void setNeverShowState(Boolean neverShowState) {
            isNeverShowState = neverShowState;
        }

        public Boolean getPrivate() {
            return isPrivate;
        }

        public void setPrivate(Boolean aPrivate) {
            isPrivate = aPrivate;
        }

        public String getTelop() {
            return telop;
        }

        public void setTelop(String telop) {
            this.telop = telop;
        }
    }

    @Override
    public boolean isMatch(URI channelUrl) {
        return channelUrl.getHost().contains("twitcasting.tv");
    }
}
