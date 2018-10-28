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

import site.alice.liveman.mediaproxy.MediaProxyManager;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.mediaproxy.proxytask.MediaProxyTask;

import java.net.URI;

public abstract class LiveService {

    public URI getLiveVideoAddress(ChannelInfo channelInfo) throws Exception {
        VideoInfo videoInfo = getLiveVideoInfo(channelInfo);
        if (videoInfo != null) {
            MediaProxyTask mediaProxyTask = MediaProxyManager.createProxy(videoInfo);
            channelInfo.addProxyTask(mediaProxyTask);
            return mediaProxyTask.getTargetUrl();
        } else {
            return null;
        }
    }

    protected abstract VideoInfo getLiveVideoInfo(ChannelInfo channelInfo) throws Exception;

    protected abstract boolean isMatch(URI channelUrl);
}
