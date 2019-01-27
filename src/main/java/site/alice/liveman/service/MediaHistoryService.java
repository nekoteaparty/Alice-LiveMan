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

package site.alice.liveman.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;
import site.alice.liveman.event.MediaProxyEvent;
import site.alice.liveman.event.MediaProxyEventListener;
import site.alice.liveman.mediaproxy.MediaProxyManager;
import site.alice.liveman.mediaproxy.proxytask.MediaProxyTask;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.MediaHistory;
import site.alice.liveman.model.VideoInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class MediaHistoryService {

    private static final File                      HISTORY_FILE    = new File("history.txt");
    private static final Map<String, MediaHistory> mediaHistoryMap = new ConcurrentHashMap<>();

    static {
        try {
            if (HISTORY_FILE.exists()) {
                List<String> historyList = IOUtils.readLines(new FileInputStream(HISTORY_FILE), StandardCharsets.UTF_8);
                for (String historyLine : historyList) {
                    try {
                        String[] split = historyLine.split("\\|");
                        MediaHistory mediaHistory = new MediaHistory();
                        mediaHistory.setVideoId(split[0]);
                        mediaHistory.setVideoTitle(split[1]);
                        mediaHistory.setChannelName(split[2]);
                        mediaHistory.setDatetime(new Date(Long.parseLong(split[3])));
                        mediaHistory.setNeedRecord(true);
                        mediaHistory.setPostDynamic(true);
                        mediaHistoryMap.put(mediaHistory.getVideoId(), mediaHistory);
                    } catch (Exception e) {
                        log.error("读取转播历史信息出错[" + historyLine + "]", e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("初始化转播历史信息失败", e);
        }
        MediaProxyManager.addListener(new MediaProxyEventListener() {
            @Override
            public void onProxyStart(MediaProxyEvent e) {
                MediaProxyTask task = e.getMediaProxyTask();
                VideoInfo videoInfo = task.getVideoInfo();
                if (videoInfo != null && !mediaHistoryMap.containsKey(videoInfo.getVideoId())) {
                    ChannelInfo channelInfo = videoInfo.getChannelInfo();
                    MediaHistory mediaHistory = new MediaHistory();
                    mediaHistory.setNeedRecord(videoInfo.isNeedRecord());
                    mediaHistory.setVideoId(videoInfo.getVideoId());
                    mediaHistory.setVideoTitle(videoInfo.getTitle());
                    mediaHistory.setChannelName(channelInfo.getChannelName());
                    mediaHistory.setDatetime(new Date());
                    mediaHistoryMap.put(videoInfo.getVideoId(), mediaHistory);
                    synchronized (this) {
                        try (OutputStream os = new FileOutputStream("history.txt", true)) {
                            IOUtils.write(String.format("%s|%s|%s|%s\n", videoInfo.getVideoId(), videoInfo.getTitle(), channelInfo.getChannelName(), System.currentTimeMillis()), os, StandardCharsets.UTF_8);
                        } catch (Exception err) {
                            log.error("保存历史记录失败", err);
                        }
                    }
                }
            }
        });
    }

    public MediaHistory getMediaHistory(String videoId) {
        MediaHistory mediaHistory = mediaHistoryMap.get(videoId);
        if (mediaHistory == null) {
            log.warn("没有找到[videoId=" + videoId + "]的历史媒体信息");
        }
        return mediaHistory;
    }

}
