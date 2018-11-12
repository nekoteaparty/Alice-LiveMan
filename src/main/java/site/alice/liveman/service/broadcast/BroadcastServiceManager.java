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
package site.alice.liveman.service.broadcast;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import site.alice.liveman.event.MediaProxyEvent;
import site.alice.liveman.event.MediaProxyEventListener;
import site.alice.liveman.mediaproxy.MediaProxyManager;
import site.alice.liveman.model.AccountInfo;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.utils.BilibiliApiUtil;
import site.alice.liveman.utils.FfmpegUtil;
import site.alice.liveman.utils.ProcessUtil;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class BroadcastServiceManager implements ApplicationContextAware {
    private static final ThreadPoolExecutor            threadPoolExecutor = new ThreadPoolExecutor(50, 50, 100000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(10));
    private              Map<String, BroadcastService> broadcastServiceMap;
    @Autowired
    private              LiveManSetting                liveManSetting;
    @Autowired
    private              FfmpegUtil                    ffmpegUtil;
    @Autowired
    private              BilibiliApiUtil               bilibiliApiUtil;

    @PostConstruct
    public void init() {
        MediaProxyManager.addListener(new MediaProxyEventListener() {
            @Override
            public void onProxyStart(MediaProxyEvent e) {
                VideoInfo videoInfo = e.getMediaProxyTask().getVideoInfo();
                if (videoInfo != null) {
                    threadPoolExecutor.execute(new BroadcastTask(videoInfo));
                }
            }

            @Override
            public void onProxyStop(MediaProxyEvent e) {
                VideoInfo videoInfo = e.getMediaProxyTask().getVideoInfo();
                List<AccountInfo> accounts = liveManSetting.getAccounts();
                for (AccountInfo accountInfo : accounts) {
                    if (accountInfo.removeCurrentVideo(videoInfo)) {
                        return;
                    }
                }
            }
        });
    }

    public AccountInfo getBroadcastAccount(VideoInfo videoInfo) {
        ChannelInfo channelInfo = videoInfo.getChannelInfo();
        String defaultAccountId = channelInfo.getDefaultAccountId();
        if (defaultAccountId != null) {
            AccountInfo accountInfo = liveManSetting.findByAccountId(defaultAccountId);
            if (accountInfo != null && accountInfo.setCurrentVideo(videoInfo)) {
                return accountInfo;
            }
        }
        if (channelInfo.isAutoBalance()) {
            /* 默认直播间不可用或没有设置默认 */
            List<AccountInfo> accounts = liveManSetting.getAccounts();
            for (AccountInfo accountInfo : accounts) {
                if (accountInfo.isJoinAutoBalance() && accountInfo.setCurrentVideo(videoInfo)) {
                    return accountInfo;
                }
            }
        }
        throw new RuntimeException("频道[" + channelInfo.getChannelName() + "], videoId=" + videoInfo.getVideoId() + "没有找到可以推流的直播间");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        broadcastServiceMap = applicationContext.getBeansOfType(BroadcastService.class);
    }

    public BroadcastService getBroadcastService(String accountSite) {
        for (BroadcastService broadcastService : broadcastServiceMap.values()) {
            if (broadcastService.isMatch(accountSite)) {
                return broadcastService;
            }
        }
        throw new BeanDefinitionStoreException("没有找到可以推流到[" + accountSite + "]的BroadcastService");
    }

    class BroadcastTask implements Runnable {

        private VideoInfo videoInfo;

        public BroadcastTask(VideoInfo videoInfo) {
            this.videoInfo = videoInfo;
        }

        @Override
        public void run() {
            while (MediaProxyManager.getExecutedProxyTaskMap().containsKey(videoInfo.getVideoId())) {
                try {
                    AccountInfo broadcastAccount = getBroadcastAccount(videoInfo);
                    bilibiliApiUtil.postDynamic(broadcastAccount);
                    while (broadcastAccount.getCurrentVideo() == videoInfo) {
                        try {
                            VideoInfo currentVideo = broadcastAccount.getCurrentVideo();
                            String broadcastAddress = getBroadcastService(broadcastAccount.getAccountSite()).getBroadcastAddress(broadcastAccount);
                            String ffmpegCmdLine = ffmpegUtil.buildFfmpegCmdLine(currentVideo, broadcastAddress);
                            long pid = ProcessUtil.createProcess(ffmpegUtil.getFfmpegPath(), ffmpegCmdLine, false);
                            log.info("[" + broadcastAccount.getRoomId() + "@" + broadcastAccount.getAccountSite() + ", videoId=" + currentVideo.getVideoId() + "]推流进程已启动[PID:" + pid + "][" + ffmpegCmdLine + "]");
                            // 等待进程退出或者任务结束
                            while (broadcastAccount.getCurrentVideo() != null && !ProcessUtil.waitProcess(pid, 1000)) ;
                            // 杀死进程
                            ProcessUtil.killProcess(pid);
                            log.info("[" + broadcastAccount.getRoomId() + "@" + broadcastAccount.getAccountSite() + ", videoId=" + currentVideo.getVideoId() + "]推流进程已终止PID:" + pid);
                        } catch (Throwable e) {
                            log.error("startBroadcast failed", e);
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ignore) {
                            }
                        }
                    }
                } catch (Throwable e) {
                    log.error("startBroadcast failed", e);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ignore) {
                    }
                }
            }
        }
    }
}
