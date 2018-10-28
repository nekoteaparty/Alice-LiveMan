package site.alicemononobe.liveman.mediaproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import site.alicemononobe.liveman.event.MediaProxyEvent;
import site.alicemononobe.liveman.event.MediaProxyEventListener;
import site.alicemononobe.liveman.mediaproxy.proxytask.MediaProxyTask;
import site.alicemononobe.liveman.model.VideoInfo;

import java.io.IOException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MediaProxyManager implements ApplicationContextAware {
    private static final Logger                        LOGGER               = LoggerFactory.getLogger(MediaProxyManager.class);
    private static final ThreadPoolExecutor            threadPoolExecutor   = new ThreadPoolExecutor(20, 20, 100000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(50));
    private static final Map<String, MediaProxyTask>   executedProxyTaskMap = new HashMap<>();
    private static final List<MediaProxyEventListener> listeners            = new ArrayList<>();
    private static       Map<String, MediaProxy>       proxyMap;
    private static       String                        tempPath;
    private static final String                        targetUrlFormat      = "http://localhost:8080/mediaProxy/%s/%s";

    public static String getTempPath() {
        return tempPath;
    }

    @Value("${media.proxy.temp.path}")
    public void setTempPath(String tempPath) {
        MediaProxyManager.tempPath = tempPath;
    }

    public static MediaProxyTask createProxy(VideoInfo videoInfo) throws Exception {
        MediaProxyTask mediaProxyTask = createProxy(videoInfo.getVideoId(), videoInfo.getMediaUrl(), videoInfo.getMediaFormat(), videoInfo.getNetworkProxy());
        mediaProxyTask.setVideoInfo(videoInfo);
        return mediaProxyTask;
    }

    public static MediaProxyTask createProxy(String videoId, URI sourceUrl, String requestFormat, Proxy proxy) throws Exception {
        for (Map.Entry<String, MediaProxy> metaProxyEntry : proxyMap.entrySet()) {
            MediaProxy metaProxy = metaProxyEntry.getValue();
            if (metaProxy.isMatch(sourceUrl, requestFormat)) {
                MediaProxyTask mediaProxyTask = metaProxy.createProxyTask(videoId, sourceUrl, proxy);
                String proxyName = metaProxyEntry.getKey().replace("MediaProxy", "");
                String targetUrl = String.format(targetUrlFormat, proxyName, videoId);
                mediaProxyTask.setTargetUrl(new URI(targetUrl));
                MediaProxyManager.runProxy(mediaProxyTask);
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
                MediaProxyEvent mediaProxyEvent = new MediaProxyEvent(MediaProxyManager.class);
                mediaProxyEvent.setMediaProxyTask(task);
                listener.onProxyStart(mediaProxyEvent);
            }
        } else {
            MediaProxyTask mediaProxyTask = executedProxyTaskMap.get(task.getVideoId());
            mediaProxyTask.setSourceUrl(task.getSourceUrl());
            mediaProxyTask.setProxy(task.getProxy());
        }
    }

    public static void removeProxy(MediaProxyTask task) {
        LOGGER.info("停止节目直播流代理[" + task.getVideoId() + "]");
        executedProxyTaskMap.remove(task.getVideoId());
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
    }
}
