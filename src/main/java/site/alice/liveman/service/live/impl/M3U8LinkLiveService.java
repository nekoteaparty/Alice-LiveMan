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
import java.util.UUID;

@Service
public class M3U8LinkLiveService extends LiveService {
    @Override
    public URI getLiveVideoInfoUrl(ChannelInfo channelInfo) throws Exception {
        return new URI(channelInfo.getChannelUrl());
    }

    @Override
    public VideoInfo getLiveVideoInfo(URI videoInfoUrl, ChannelInfo channelInfo, String resolution) throws Exception {
        String m3u8 = HttpRequestUtil.downloadUrl(videoInfoUrl, channelInfo.getCookies(), Collections.emptyMap(), StandardCharsets.UTF_8);
        if (m3u8.contains("#EXTM3U")) {
            return new VideoInfo(channelInfo, UUID.randomUUID().toString(), videoInfoUrl.toString(), videoInfoUrl, videoInfoUrl, "m3u8");
        } else {
            throw new RuntimeException(videoInfoUrl + "文件无效");
        }
    }

    @Override
    protected boolean isMatch(URI channelUrl) {
        return channelUrl.toString().contains(".m3u8");
    }
}
