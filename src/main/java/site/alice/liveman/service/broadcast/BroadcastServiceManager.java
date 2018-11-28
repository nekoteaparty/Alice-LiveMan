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
import site.alice.liveman.mediaproxy.proxytask.MediaProxyTask;
import site.alice.liveman.model.AccountInfo;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.utils.BilibiliApiUtil;
import site.alice.liveman.utils.FfmpegUtil;
import site.alice.liveman.utils.ProcessUtil;
import site.alice.liveman.web.dataobject.ActionResult;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private              BilibiliApiUtil               bilibiliApiUtil;

    @PostConstruct
    public void init() {
        MediaProxyManager.addListener(new MediaProxyEventListener() {
            @Override
            public void onProxyStart(MediaProxyEvent e) {
                VideoInfo videoInfo = e.getMediaProxyTask().getVideoInfo();
                if (videoInfo != null) {
                    BroadcastTask broadcastTask;
                    if (videoInfo.getBroadcastTask() == null) {
                        broadcastTask = new BroadcastTask(videoInfo);
                        if (!videoInfo.setBroadcastTask(broadcastTask)) {
                            BroadcastTask currentBroadcastTask = videoInfo.getBroadcastTask();
                            try {
                                log.warn("试图创建推流任务的媒体资源已存在推流任务[roomId={}]，这是不正常的意外情况，将尝试终止已存在的推流任务[videoId={}]", currentBroadcastTask.getBroadcastAccount().getRoomId(), videoInfo.getVideoId());
                                if (!currentBroadcastTask.terminateTask()) {
                                    log.warn("终止转播任务失败：CAS操作失败");
                                }
                            } catch (Throwable throwable) {
                                log.error("启动推流任务时发生异常", throwable);
                            }
                        }
                    } else {
                        broadcastTask = videoInfo.getBroadcastTask();
                    }
                    threadPoolExecutor.execute(broadcastTask);
                }
            }

            @Override
            public void onProxyStop(MediaProxyEvent e) {
                VideoInfo videoInfo = e.getMediaProxyTask().getVideoInfo();
                if (videoInfo != null) {
                    BroadcastTask broadcastTask = videoInfo.getBroadcastTask();
                    if (broadcastTask != null) {
                        AccountInfo broadcastAccount = broadcastTask.getBroadcastAccount();
                        if (broadcastAccount != null) {
                            broadcastAccount.removeCurrentVideo(videoInfo);
                            videoInfo.removeBroadcastTask(broadcastTask);
                        }
                    }
                }
            }
        });
    }

    public BroadcastTask createSingleBroadcastTask(VideoInfo videoInfo, AccountInfo broadcastAccount) throws Exception {
        if (broadcastAccount.setCurrentVideo(videoInfo)) {
            try {
                Map<String, MediaProxyTask> executedProxyTaskMap = MediaProxyManager.getExecutedProxyTaskMap();
                // 如果要推流的媒体已存在，则提示错误信息
                MediaProxyTask mediaProxyTask = executedProxyTaskMap.get(videoInfo.getVideoId());
                if (mediaProxyTask != null) {
                    throw new RuntimeException("此媒体已在推流任务列表中，无法添加");
                } else {
                    // 创建直播流代理任务
                    BroadcastTask broadcastTask = new BroadcastTask(videoInfo, broadcastAccount);
                    if (!videoInfo.setBroadcastTask(broadcastTask)) {
                        throw new RuntimeException("此媒体已在推流任务列表中，无法添加");
                    }
                    mediaProxyTask = MediaProxyManager.createProxy(videoInfo);
                    if (mediaProxyTask == null) {
                        throw new RuntimeException("MediaProxyTask创建失败");
                    }
                    return broadcastTask;
                }
            } catch (Exception e) {
                // 操作失败，释放刚才获得的直播间资源
                broadcastAccount.removeCurrentVideo(videoInfo);
                throw e;
            }
        } else {
            throw new RuntimeException("无法创建转播任务，直播间已被节目[" + broadcastAccount.getCurrentVideo().getTitle() + "]占用！");
        }
    }

    public AccountInfo getBroadcastAccount(VideoInfo videoInfo) {
        ChannelInfo channelInfo = videoInfo.getChannelInfo();
        String defaultAccountId = channelInfo.getDefaultAccountId();
        if (defaultAccountId != null) {
            AccountInfo accountInfo = liveManSetting.findByAccountId(defaultAccountId);
            String logInfo = "频道[" + channelInfo.getChannelName() + "], videoId=" + videoInfo.getVideoId() + "的默认直播间[" + defaultAccountId + "]";
            if (accountInfo == null) {
                log.info(logInfo + "的账号信息不存在");
            } else if (accountInfo.isDisable()) {
                log.info(logInfo + "的账号信息不可用");
            } else if (!accountInfo.setCurrentVideo(videoInfo)) {
                log.info(logInfo + "已被占用[currentVideo=" + accountInfo.getCurrentVideo().getVideoId() + "]");
            } else {
                return accountInfo;
            }
        }
        if (channelInfo.isAutoBalance()) {
            /* 默认直播间不可用或没有设置默认 */
            Set<AccountInfo> accounts = liveManSetting.getAccounts();
            for (AccountInfo accountInfo : accounts) {
                if (accountInfo.isJoinAutoBalance() && !accountInfo.isDisable() && accountInfo.setCurrentVideo(videoInfo)) {
                    return accountInfo;
                }
            }
        }
        log.info("频道[" + channelInfo.getChannelName() + "], videoId=" + videoInfo.getVideoId() + "没有找到可以推流的直播间");
        return null;
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

    public class BroadcastTask implements Runnable {

        private VideoInfo   videoInfo;
        private long        pid;
        private AccountInfo broadcastAccount;
        private boolean     terminate;
        private boolean     singleTask;

        public BroadcastTask(VideoInfo videoInfo, AccountInfo broadcastAccount) {
            this.videoInfo = videoInfo;
            this.broadcastAccount = broadcastAccount;
            singleTask = true;
        }

        public BroadcastTask(VideoInfo videoInfo) {
            this.videoInfo = videoInfo;
        }

        public VideoInfo getVideoInfo() {
            return videoInfo;
        }

        public long getPid() {
            return pid;
        }

        public AccountInfo getBroadcastAccount() {
            return broadcastAccount;
        }

        public boolean isTerminate() {
            return terminate;
        }

        public boolean isSingleTask() {
            return singleTask;
        }

        @Override
        public void run() {
            while (MediaProxyManager.getExecutedProxyTaskMap().containsKey(videoInfo.getVideoId()) && !terminate) {
                try {
                    if (!singleTask) {
                        broadcastAccount = BroadcastServiceManager.this.getBroadcastAccount(videoInfo);
                        if (broadcastAccount == null) {
                            Thread.sleep(5000);
                            continue;
                        }
                        bilibiliApiUtil.postDynamic(broadcastAccount);
                    }
                    while (broadcastAccount.getCurrentVideo() == videoInfo && !broadcastAccount.isDisable()) {
                        try {
                            VideoInfo currentVideo = broadcastAccount.getCurrentVideo();
                            String broadcastAddress = getBroadcastService(broadcastAccount.getAccountSite()).getBroadcastAddress(broadcastAccount);
                            String ffmpegCmdLine = FfmpegUtil.buildFfmpegCmdLine(currentVideo, broadcastAddress);
                            pid = ProcessUtil.createProcess(liveManSetting.getFfmpegPath(), ffmpegCmdLine, false);
                            log.info("[" + broadcastAccount.getRoomId() + "@" + broadcastAccount.getAccountSite() + ", videoId=" + currentVideo.getVideoId() + "]推流进程已启动[PID:" + pid + "][" + ffmpegCmdLine.replace("\t", " ") + "]");
                            // 等待进程退出或者任务结束
                            while (broadcastAccount.getCurrentVideo() != null && !ProcessUtil.waitProcess(pid, 1000)) ;
                            // 杀死进程
                            ProcessUtil.killProcess(pid);
                            log.info("[" + broadcastAccount.getRoomId() + "@" + broadcastAccount.getAccountSite() + ", videoId=" + currentVideo.getVideoId() + "]推流进程已终止PID:" + pid);
                        } catch (Throwable e) {
                            log.error("startBroadcast failed", e);
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignore) {
                        }
                    }
                    broadcastAccount.removeCurrentVideo(videoInfo);
                    if (broadcastAccount.isDisable() && singleTask) {
                        log.warn("手动推流的直播账号[" + broadcastAccount.getAccountId() + "]不可用，已终止推流任务。");
                        terminate = true;
                        break;
                    }
                } catch (Throwable e) {
                    log.error("startBroadcast failed", e);
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignore) {
                }
            }
            videoInfo.removeBroadcastTask(this);
        }

        public boolean terminateTask() {
            if (broadcastAccount != null) {
                log.info("强制终止节目[" + videoInfo.getTitle() + "][videoId=" + videoInfo.getVideoId() + "]的推流任务[roomId=" + broadcastAccount.getRoomId() + "]");
                if (!broadcastAccount.removeCurrentVideo(videoInfo)) {
                    log.error("无法移除账号[" + broadcastAccount.getAccountId() + "]正在转播的节目[" + broadcastAccount.getCurrentVideo().getVideoId() + "]，目标节目与预期节目[" + videoInfo.getVideoId() + "]不符");
                    return false;
                }
            }
            terminate = true;
            videoInfo.removeBroadcastTask(this);
            ProcessUtil.killProcess(pid);
            return true;
        }
    }

}
