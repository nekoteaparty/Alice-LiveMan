package site.alicemononobe.liveman.service;

import charlie.bililivelib.danmaku.DanmakuReceiver;
import charlie.bililivelib.danmaku.datamodel.Danmaku;
import charlie.bililivelib.danmaku.dispatch.DanmakuDispatcher;
import charlie.bililivelib.danmaku.event.DanmakuAdapter;
import charlie.bililivelib.danmaku.event.DanmakuEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import site.alicemononobe.liveman.event.DemandEvent;
import site.alicemononobe.liveman.event.DemandEventListener;
import site.alicemononobe.liveman.event.MediaProxyEvent;
import site.alicemononobe.liveman.event.MediaProxyEventListener;
import site.alicemononobe.liveman.model.ChannelInfo;
import site.alicemononobe.liveman.model.VideoInfo;
import site.alicemononobe.liveman.mediaproxy.MediaProxyManager;
import site.alicemononobe.liveman.mediaproxy.MediaProxy;
import site.alicemononobe.liveman.mediaproxy.proxytask.MediaProxyTask;
import site.alicemononobe.liveman.utils.DynamicAreaUtil;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DanmakuDemandService implements DemandEventListener, MediaProxyEventListener {

    private static final Logger                    LOGGER           = LoggerFactory.getLogger(DanmakuDemandService.class);
    private static final Map<String, Integer>      demandCountMap   = new ConcurrentHashMap<>();
    private static final Set<Integer>              demandUserIdSet  = new HashSet<>();
    private static       long                      NEXT_RESET_TIME  = System.currentTimeMillis();
    private              DanmakuReceiver           danmakuReceiver;
    private static final List<DemandEventListener> listeners        = new ArrayList<>();
    private static final String                    dynamicTipFormat = "当前点播信息：%s\n发送对应VTuber直播间名称点播（可以从简介中复制），点播有效期1分钟，1分钟后点播权重减半\n创建和谐社会，当直播节目名带有 %s 关键字时将不会转播。";
    @Autowired
    private              MediaProxyManager         mediaProxyManager;
    @Value("${bili.banned.keywords}")
    private              String[]                  bannedKeywords;

    public MediaProxyTask getMediaProxyTask() {
        return mediaProxyTask;
    }

    public void setMediaProxyTask(MediaProxyTask mediaProxyTask) {
        this.mediaProxyTask = mediaProxyTask;
    }

    private MediaProxyTask mediaProxyTask = null;

    @Bean
    public DanmakuReceiver getDanmakuReceiver(@Value("${bili.live.room.id}") int roomId) {
        return new DanmakuReceiver(roomId);
    }

    @Autowired
    public void setDanmakuReceiver(DanmakuReceiver danmakuReceiver) {
        this.danmakuReceiver = danmakuReceiver;
        listeners.add(this);
        MediaProxyManager.addListener(this);
        danmakuReceiver.getDispatchManager().registerDispatcher(new DanmakuDispatcher());
        danmakuReceiver.addDanmakuListener(new DanmakuAdapter() {
            @Override
            public void danmakuEvent(@NotNull DanmakuEvent event) {
                Danmaku danmaku = (Danmaku) event.getParam();
                int userId = danmaku.getUser().getUid();
                String danmakuContent = danmaku.getContent();
                if (demandCountMap.containsKey(danmakuContent)) {
                    if (NEXT_RESET_TIME <= System.currentTimeMillis()) {
                        for (Entry<String, Integer> entry : demandCountMap.entrySet()) {
                            if (entry.getValue() > 0) {
                                entry.setValue(entry.getValue() / 2);
                            }
                        }
                        NEXT_RESET_TIME = System.currentTimeMillis() + 60000;
                        demandUserIdSet.clear();
                    }
                    if (demandUserIdSet.contains(userId)) {
                        return;
                    }
                    demandUserIdSet.add(userId);
                    demandCountMap.put(danmakuContent, demandCountMap.get(danmakuContent) + 1);
                    for (DemandEventListener listener : listeners) {
                        DemandEvent demandEvent = new DemandEvent(this);
                        demandEvent.setCurrentDemandItem(danmakuContent);
                        demandEvent.setDemandCountMap(Collections.unmodifiableMap(demandCountMap));
                        listener.onAddDemand(demandEvent);
                    }
                }
            }
        });
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                danmakuReceiver.connect();
            }
        }, 0, 5000);
        for (DemandEventListener listener : listeners) {
            listener.onDemandStart();
        }
    }

    public DanmakuReceiver getDanmakuReceiver() {
        return danmakuReceiver;
    }

    public void addDemandItem(String demandItem) {
        if (!demandCountMap.containsKey(demandItem)) {
            demandCountMap.put(demandItem, 0);
        }
    }

    public void removeDemandItem(String demandItem) {
        demandCountMap.remove(demandItem);
    }

    public void sortByDemand(List<ChannelInfo> channelInfoList) {
        channelInfoList.sort((o1, o2) -> demandCountMap.getOrDefault(o2.getChannelName(), 0).compareTo(demandCountMap.getOrDefault(o1.getChannelName(), 0)));
        for (int i = 0; i < channelInfoList.size(); i++) {
            ChannelInfo channelInfo = channelInfoList.get(i);
            channelInfo.setSort(i);
        }
    }

    @Override
    public void onAddDemand(DemandEvent demandEvent) {
        Map<String, Integer> demandCountMap = demandEvent.getDemandCountMap();
        StringBuilder sb = new StringBuilder();
        for (Entry<String, Integer> entry : demandCountMap.entrySet()) {
            if (entry.getValue() > 0) {
                sb.append(entry.getKey()).append("(").append(entry.getValue()).append(") ");
            }
        }
        try {
            File areaImage = DynamicAreaUtil.createAreaImage(String.format(dynamicTipFormat, sb, String.join("、", bannedKeywords)), 1920, 180);
            if (mediaProxyTask != null) {
                mediaProxyTask.terminate();
            }
            Thread.sleep(1000);
            mediaProxyTask = MediaProxyManager.createProxy(areaImage.getName(), new URI("file://" + URLEncoder.encode(areaImage.getAbsolutePath(), "utf-8")), "flv", null);
        } catch (Exception e) {
            LOGGER.error("onAddDemand()", e);
        }
    }

    @Override
    public void onDemandStart() {
        try {
            File areaImage = DynamicAreaUtil.createAreaImage(String.format(dynamicTipFormat, "[无]", String.join("、", bannedKeywords)), 1920, 180);
            if (mediaProxyTask != null) {
                mediaProxyTask.terminate();
            }
            Thread.sleep(1000);
            mediaProxyTask = MediaProxyManager.createProxy(areaImage.getName(), new URI("file://" + URLEncoder.encode(areaImage.getAbsolutePath(), "utf-8")), "flv", null);
        } catch (Exception e) {
            LOGGER.error("onDemandStart()", e);
        }
    }

    @Override
    public void onProxyStart(MediaProxyEvent e) {
        VideoInfo videoInfo = e.getMediaProxyTask().getVideoInfo();
        if (videoInfo != null) {
            addDemandItem(videoInfo.getChannelInfo().getChannelName());
        }
    }

    @Override
    public void onProxyStop(MediaProxyEvent e) {
        VideoInfo videoInfo = e.getMediaProxyTask().getVideoInfo();
        if (videoInfo != null) {
            removeDemandItem(videoInfo.getChannelInfo().getChannelName());
        }
    }
}
