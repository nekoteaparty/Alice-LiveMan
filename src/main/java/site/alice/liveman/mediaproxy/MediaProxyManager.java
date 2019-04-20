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
package site.alice.liveman.mediaproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import site.alice.liveman.event.MediaProxyEvent;
import site.alice.liveman.event.MediaProxyEventListener;
import site.alice.liveman.mediaproxy.proxytask.MediaProxyTask;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.service.VideoFilterService;
import site.alice.liveman.utils.ThreadPoolUtil;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

public class MediaProxyManager implements ApplicationContextAware {
    private static final Logger                        LOGGER               = LoggerFactory.getLogger(MediaProxyManager.class);
    private static final Map<String, MediaProxyTask>   executedProxyTaskMap = new ConcurrentHashMap<>();
    private static final List<MediaProxyEventListener> listeners            = new CopyOnWriteArrayList<>();
    private static       Map<String, MediaProxy>       proxyMap;
    private static       String                        tempPath;
    private static final String                        targetUrlFormat      = "http://" + getIpAddress() + ":8080/mediaProxy/%s/%s";
    private static       ApplicationContext            applicationContext;

    public static String getTempPath() {
        return tempPath;
    }

    @Autowired
    public void setTempPath(LiveManSetting liveManSetting) {
        MediaProxyManager.tempPath = liveManSetting.getTempPath();
    }

    public static MediaProxyTask createProxy(VideoInfo videoInfo) throws Exception {
        MediaProxyTask mediaProxyTask = createProxyTask(videoInfo, videoInfo.getMediaUrl(), videoInfo.getMediaFormat());
        mediaProxyTask.setVideoInfo(videoInfo);
        ChannelInfo channelInfo = videoInfo.getChannelInfo();
        if (channelInfo != null) {
            videoInfo.setArea(channelInfo.getDefaultArea());
        }
        videoInfo.setMediaProxyUrl(mediaProxyTask.getTargetUrl());
        runProxy(mediaProxyTask);
        return mediaProxyTask;
    }

    private static MediaProxyTask createProxyTask(VideoInfo videoInfo, URI sourceUrl, String requestFormat) throws Exception {
        for (Map.Entry<String, MediaProxy> metaProxyEntry : proxyMap.entrySet()) {
            MediaProxy metaProxy = metaProxyEntry.getValue();
            if (metaProxy.isMatch(sourceUrl, requestFormat)) {
                MediaProxyTask mediaProxyTask = metaProxy.createProxyTask(videoInfo.getVideoId(), sourceUrl);
                applicationContext.getAutowireCapableBeanFactory().autowireBean(mediaProxyTask);
                String proxyName = metaProxyEntry.getKey().replace("MediaProxy", "");
                String targetUrl = String.format(targetUrlFormat, proxyName, videoInfo.getVideoUnionId());
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
        MediaProxyTask mediaProxyTask = executedProxyTaskMap.get(task.getVideoId());
        if (mediaProxyTask == null) {
            LOGGER.info("开始节目直播流代理[" + task.getVideoId() + "]" + (task.getSourceUrl() != null ? "[sourceUrl=" + task.getSourceUrl() + ", targetUrl=" + task.getTargetUrl() + "]" : ""));
            executedProxyTaskMap.put(task.getVideoId(), task);
            ThreadPoolUtil.execute(task);
            for (MediaProxyEventListener listener : listeners) {
                try {
                    MediaProxyEvent mediaProxyEvent = new MediaProxyEvent(MediaProxyManager.class);
                    mediaProxyEvent.setMediaProxyTask(task);
                    listener.onProxyStart(mediaProxyEvent);
                } catch (Exception e) {
                    LOGGER.error("调用" + listener + "失败", e);
                }
            }
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

    public static String getIpAddress() {
        try {
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress ip;
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = allNetInterfaces.nextElement();
                if (!netInterface.isLoopback() && !netInterface.isVirtual() && netInterface.isUp()) {
                    Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        ip = addresses.nextElement();
                        if (ip instanceof Inet4Address) {
                            LOGGER.info("MediaProxy Server IPAddress:" + ip.getHostAddress());
                            return ip.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("IP地址获取失败", e);
        }
        return "127.0.0.1";
    }

}
