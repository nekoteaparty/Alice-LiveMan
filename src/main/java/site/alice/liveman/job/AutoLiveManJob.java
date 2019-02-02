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
package site.alice.liveman.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import site.alice.liveman.mediaproxy.proxytask.MediaProxyTask;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.service.live.LiveServiceFactory;

import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class AutoLiveManJob {
    private static final Logger             LOGGER = LoggerFactory.getLogger(AutoLiveManJob.class);
    @Autowired
    private              LiveServiceFactory liveServiceFactory;
    @Autowired
    private              LiveManSetting     liveManSetting;

    @Scheduled(cron = "0/5 * * * * ?")
    public void aliceLiveJob() {
        if (liveManSetting.getChannels().isEmpty()) {
            LOGGER.warn("频道列表为空！");
        }
        /* 获取频道状态信息 */
        CopyOnWriteArraySet<ChannelInfo> channels = liveManSetting.getChannels();
        channels.parallelStream().forEach(channelInfo -> {
            MediaProxyTask mediaProxyTask;
            try {
                mediaProxyTask = liveServiceFactory.getLiveService(channelInfo.getChannelUrl()).createMediaProxyTask(channelInfo, liveManSetting.getDefaultResolution());
                if (mediaProxyTask != null) {
                    LOGGER.info(channelInfo.getChannelName() + "[" + channelInfo.getChannelUrl() + "]正在直播，媒体地址:" + mediaProxyTask.getSourceUrl());
                } else {
                    LOGGER.info(channelInfo.getChannelName() + "[" + channelInfo.getChannelUrl() + "]没有正在直播的节目");
                }
                Thread.sleep(1000);
            } catch (Throwable e) {
                LOGGER.error("获取 " + channelInfo.getChannelName() + "[" + channelInfo.getChannelUrl() + "] 频道信息失败", e);
            }
        });
    }
}

