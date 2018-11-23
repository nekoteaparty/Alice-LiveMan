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
package site.alice.liveman.mediaproxy;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import site.alice.liveman.event.MediaProxyEvent;
import site.alice.liveman.event.MediaProxyEventListener;
import site.alice.liveman.mediaproxy.proxytask.MediaProxyTask;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.service.VideoFilterService;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.Proxy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class MediaProxyManager implements ApplicationContextAware {
    private static final Logger                        LOGGER               = LoggerFactory.getLogger(MediaProxyManager.class);
    private static final ThreadPoolExecutor            threadPoolExecutor   = new ThreadPoolExecutor(50, 50, 100000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(10));
    private static final Map<String, MediaProxyTask>   executedProxyTaskMap = new ConcurrentHashMap<>();
    private static final List<MediaProxyEventListener> listeners            = new CopyOnWriteArrayList<>();
    private static       Map<String, MediaProxy>       proxyMap;
    private static       String                        tempPath;
    private static final String                        targetUrlFormat      = "http://localhost:8080/mediaProxy/%s/%s";
    private static       ApplicationContext            applicationContext;
    private static       VideoFilterService            videoFilterService;

    @Autowired
    public void setVideoFilterService(VideoFilterService videoFilterService) {
        MediaProxyManager.videoFilterService = videoFilterService;
    }

    public static String getTempPath() {
        return tempPath;
    }

    @Autowired
    public void setTempPath(LiveManSetting liveManSetting) {
        MediaProxyManager.tempPath = liveManSetting.getTempPath();
    }

    public static MediaProxyTask createProxy(VideoInfo videoInfo) throws Exception {
        MediaProxyTask mediaProxyTask = createProxyTask(videoInfo.getVideoId(), videoInfo.getMediaUrl(), videoInfo.getMediaFormat());
        mediaProxyTask.setVideoInfo(videoInfo);
        ChannelInfo channelInfo = videoInfo.getChannelInfo();
        if (channelInfo == null) {
            channelInfo = new ChannelInfo();
            videoInfo.setChannelInfo(channelInfo);
        }
        channelInfo.setMediaUrl(mediaProxyTask.getTargetUrl().toString());
        channelInfo.addProxyTask(mediaProxyTask);
        videoInfo.setArea(channelInfo.getDefaultArea());
        videoFilterService.doFilter(videoInfo);
        runProxy(mediaProxyTask);
        synchronized (MediaProxyManager.class) {
            try (OutputStream os = new FileOutputStream("history.txt", true)) {
                IOUtils.write(String.format("%s|%s|%s|%s\n", videoInfo.getVideoId(), videoInfo.getTitle(), channelInfo.getChannelName(), System.currentTimeMillis()), os, StandardCharsets.UTF_8);
            } catch (Exception e) {
                LOGGER.error("保存历史记录失败", e);
            }
        }
        return mediaProxyTask;
    }

    public static MediaProxyTask createProxy(String videoId, URI sourceUrl, String requestFormat) throws Exception {
        MediaProxyTask mediaProxyTask = createProxyTask(videoId, sourceUrl, requestFormat);
        runProxy(mediaProxyTask);
        return mediaProxyTask;
    }

    private static MediaProxyTask createProxyTask(String videoId, URI sourceUrl, String requestFormat) throws Exception {
        for (Map.Entry<String, MediaProxy> metaProxyEntry : proxyMap.entrySet()) {
            MediaProxy metaProxy = metaProxyEntry.getValue();
            if (metaProxy.isMatch(sourceUrl, requestFormat)) {
                MediaProxyTask mediaProxyTask = metaProxy.createProxyTask(videoId, sourceUrl);
                applicationContext.getAutowireCapableBeanFactory().autowireBean(mediaProxyTask);
                String proxyName = metaProxyEntry.getKey().replace("MediaProxy", "");
                String targetUrl = String.format(targetUrlFormat, proxyName, videoId);
                mediaProxyTask.setTargetUrl(new URI(targetUrl));
                return mediaProxyTask;
            }
        }
        throw new RuntimeException("找不到可以处理URL[" + sourceUrl + "]的媒体代理服务");
    }

    public static MediaProxy getMediaProxy(String proxyName) {
        return proxyMap.get(proxyName + "MediaProxy");
    }

    public static <T extends MediaProxy> T getMediaProxy(Class<T> tClass) {
        Collection<MediaProxy> mediaProxies = proxyMap.values();
        for (MediaProxy mediaProxy : mediaProxies) {
            if (mediaProxy.getClass().equals(tClass)) {
                return tClass.cast(mediaProxy);
            }
        }
        return null;
    }

    public static void runProxy(MediaProxyTask task) {
        if (!executedProxyTaskMap.containsKey(task.getVideoId())) {
            LOGGER.info("开始节目直播流代理[" + task.getVideoId() + "]" + (task.getSourceUrl() != null ? "[sourceUrl=" + task.getSourceUrl() + ", targetUrl=" + task.getTargetUrl() + "]" : ""));
            executedProxyTaskMap.put(task.getVideoId(), task);
            threadPoolExecutor.execute(task);
            for (MediaProxyEventListener listener : listeners) {
                try {
                    MediaProxyEvent mediaProxyEvent = new MediaProxyEvent(MediaProxyManager.class);
                    mediaProxyEvent.setMediaProxyTask(task);
                    listener.onProxyStart(mediaProxyEvent);
                } catch (Exception e) {
                    LOGGER.error("调用" + listener + "失败", e);
                }
            }
        } else {
            MediaProxyTask mediaProxyTask = executedProxyTaskMap.get(task.getVideoId());
            mediaProxyTask.setSourceUrl(task.getSourceUrl());
        }
    }

    public static void removeProxy(MediaProxyTask task) {
        LOGGER.info("停止节目直播流代理[" + task.getVideoId() + "]");
        executedProxyTaskMap.remove(task.getVideoId());
        for (MediaProxyEventListener listener : listeners) {
            try {
                MediaProxyEvent mediaProxyEvent = new MediaProxyEvent(MediaProxyManager.class);
                mediaProxyEvent.setMediaProxyTask(task);
                listener.onProxyStop(mediaProxyEvent);
            } catch (Exception e) {
                LOGGER.error("调用" + listener + "失败", e);
            }
        }
    }

    public static Map<String, MediaProxyTask> getExecutedProxyTaskMap() {
        return Collections.unmodifiableMap(executedProxyTaskMap);
    }

    public static void addListener(MediaProxyEventListener listener) {
        listeners.add(listener);
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        proxyMap = applicationContext.getBeansOfType(MediaProxy.class);
        MediaProxyManager.applicationContext = applicationContext;
    }

}
