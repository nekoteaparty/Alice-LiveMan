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
package site.alice.liveman.job;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import site.alice.liveman.mediaproxy.MediaProxyManager;
import site.alice.liveman.mediaproxy.proxytask.FlvLivingMediaProxyTask;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.service.DanmakuDemandService;
import site.alice.liveman.utils.ProcessUtil;
import site.alice.liveman.mediaproxy.proxytask.MediaProxyTask;
import site.alice.liveman.service.live.LiveServiceFactory;
import site.alice.liveman.utils.HttpRequestUtil;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
public class AutoLiveManJob {
    private static final Logger               LOGGER              = LoggerFactory.getLogger(AutoLiveManJob.class);
    private static final int                  MAX_SUB_MOVIE_COUNT = 3;
    private static final String               BILI_START_LIVE_URL = "https://api.live.bilibili.com/room/v1/Room/startLive";
    private static       String               moviePlaceHolder    = "placeholder.flv";
    @Autowired
    private              DanmakuDemandService danmakuDemandService;
    @Autowired
    private              LiveServiceFactory   liveServiceFactory;
    @Value("${channel.config.file}")
    private              File                 channelConfigFile;
    @Value("${channel.status.file}")
    private              File                 channelStatusFile;
    @Value("${bili.banned.keywords}")
    private              String[]             bannedKeywords;
    @Value("${bili.cookie}")
    private              String               biliCookie;
    @Value("${ffmpeg.home}")
    private              String               ffmpegHome;

    @PostConstruct
    public void init() throws IOException {
        File moviePlaceFile = new File("./" + moviePlaceHolder);
        if (!moviePlaceFile.exists()) {
            ClassPathResource resource = new ClassPathResource(moviePlaceHolder);
            IOUtils.copy(resource.getInputStream(), new FileOutputStream(moviePlaceFile));
        }
        moviePlaceHolder = FilenameUtils.separatorsToUnix(moviePlaceFile.getAbsolutePath());
    }

    @Scheduled(cron = "0/1 * * * * ?")
    public void youTubeLiveJob() throws IOException, InterruptedException {
        if (!channelConfigFile.exists()) {
            channelConfigFile.createNewFile();
        }
        List<String> channels = FileUtils.readLines(channelConfigFile, StandardCharsets.UTF_8);
        if (channels.isEmpty()) {
            LOGGER.warn("频道列表为空！");
        }

        /* 获取频道状态信息 */
        for (String channel : channels) {
            String[] channelInfos = channel.split("\\|");
            if (channelInfos.length > 1) {
                String channelName = channelInfos[0];
                String channelUrl = channelInfos[1];
                URI mediaUrl;
                try {
                    ChannelInfo channelInfo = new ChannelInfo();
                    channelInfo.setChannelName(channelName);
                    channelInfo.setChannelUrl(channelUrl);
                    mediaUrl = liveServiceFactory.getLiveService(channelUrl).getLiveVideoAddress(channelInfo);
                    if (mediaUrl != null) {
                        LOGGER.info(channelName + "[" + channelUrl + "]正在直播，媒体地址:" + mediaUrl);
                        channelInfo.setMediaUrl(mediaUrl.toString());
                    } else {
                        LOGGER.info(channelName + "[" + channelUrl + "]没有正在直播的节目");
                    }
                } catch (Throwable e) {
                    LOGGER.error("获取 " + channelName + "[" + channelUrl + "] 频道信息失败", e);
                }
            }
            Thread.sleep(500);
        }
    }

    @Scheduled(cron = "0/1 * * * * ?")
    public void pushToBilibili() throws IOException {
        List<ChannelInfo> channelInfoList = new ArrayList<>();
        Map<String, MediaProxyTask> executedProxyTaskMap = MediaProxyManager.getExecutedProxyTaskMap();
        for (MediaProxyTask mediaProxyTask : executedProxyTaskMap.values()) {
            VideoInfo videoInfo = mediaProxyTask.getVideoInfo();
            if (videoInfo != null) {
                channelInfoList.add(videoInfo.getChannelInfo());
            }
        }
        File pidPath = new File(".");
        List<File> pidFiles = new ArrayList<>();
        CollectionUtils.addAll(pidFiles, pidPath.listFiles((file) -> file.getName().endsWith(".pid")));
        for (Iterator<File> iterator = pidFiles.iterator(); iterator.hasNext(); ) {
            File pidFile = iterator.next();
            if (cleanupProcess(pidFile, channelInfoList)) {
                iterator.remove();
            }
        }
        File pidFile = null;
        List<ChannelInfo> pidChannelInfoList = null;
        if (!pidFiles.isEmpty()) {
            pidFile = pidFiles.get(0);
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(pidFile))) {
                pidChannelInfoList = (List<ChannelInfo>) ois.readObject();
            } catch (ClassNotFoundException e) {
                LOGGER.error("读取当前转播进程信息失败", e);
            }
        }
        danmakuDemandService.sortByDemand(channelInfoList);
        for (ChannelInfo channelInfo : channelInfoList) {
            List<MediaProxyTask> mediaProxyTasks = channelInfo.getMediaProxyTasks();
            for (MediaProxyTask mediaProxyTask : mediaProxyTasks) {
                VideoInfo videoInfo = mediaProxyTask.getVideoInfo();
                if (videoInfo != null) {
                    String videoTitle = videoInfo.getTitle();
                    for (String bannedKeyword : bannedKeywords) {
                        if (StringUtils.containsIgnoreCase(videoTitle, bannedKeyword)) {
                            channelInfo.setMediaUrl(moviePlaceHolder);
                            if (!channelInfo.getChannelName().contains("屏蔽")) {
                                channelInfo.setChannelName(channelInfo.getChannelName() + "\n[屏蔽\\:" + bannedKeyword + "]");
                            }
                            break;
                        }
                    }
                }
            }
        }
        if (!channelInfoList.equals(pidChannelInfoList)) {
            cleanupProcess(channelInfoList);
            try {
                String startLiveJson = HttpRequestUtil.downloadUrl(new URI(BILI_START_LIVE_URL), biliCookie, "room_id=36577&platform=pc&area_v2=33", StandardCharsets.UTF_8, null);
                JSONObject startLiveObject = JSON.parseObject(startLiveJson);
                JSONObject rtmpObject;
                if (startLiveObject.get("data") instanceof JSONObject) {
                    rtmpObject = startLiveObject.getJSONObject("data").getJSONObject("rtmp");
                } else {
                    throw new Exception("开启B站直播间失败" + startLiveJson);
                }
                String biliRtmpUrl = rtmpObject.getString("addr") + rtmpObject.getString("code");
                String firstMainTitle = channelInfoList.isEmpty() ? "[空的直播位]" : " " + channelInfoList.get(0).getChannelName();
                String loopCmdLine = "-stream_loop -1 -i " + (channelInfoList.isEmpty() ? "\"" + moviePlaceHolder + "\"" : "\"" + channelInfoList.get(0).getMediaUrl() + "\"");
                String cmdLine = " -re " + loopCmdLine + " -vf \"[in]scale=-1:900, pad=1920:1080[main];[main]drawtext=x=1610:y=16+275*0:font='YaHei Consolas Hybrid':fontcolor=White:text='←" + firstMainTitle + "':fontsize=28[main_text];" +
                        "movie='%s',scale=-1:180[sub1];[main_text]drawtext=x=1610:y=16+275*1:font='YaHei Consolas Hybrid':fontcolor=White:text='↑ %s':fontsize=28[sub1_text];[sub3_text][sub1]overlay=main_w-overlay_w:95+275*0[lay1_text];" +
                        "movie='%s',scale=-1:180[sub2];[sub1_text]drawtext=x=1610:y=16+275*2:font='YaHei Consolas Hybrid':fontcolor=White:text='↑ %s':fontsize=28[sub2_text];[lay1_text][sub2]overlay=main_w-overlay_w:95+275*1[lay2_text];" +
                        "movie='%s',scale=-1:180[sub3];[sub2_text]drawtext=x=1610:y=16+275*3:font='YaHei Consolas Hybrid':fontcolor=White:text='↑ %s':fontsize=28[sub3_text];[lay2_text][sub3]overlay=main_w-overlay_w:95+275*2[lay_out];" +
                        "movie='" + danmakuDemandService.getMediaProxyTask().getTargetUrl().toString().replaceAll(":", "\\\\:") + "'[dynamic_area];[lay_out][dynamic_area]overlay=0:main_h-overlay_h[out]\"" +
                        " -vcodec h264_nvenc -acodec aac -r 30 -b:v 2500K -b:a 128K -cq 5 -f flv \"" + biliRtmpUrl + "\"";
                Object[] args = new String[MAX_SUB_MOVIE_COUNT * 2];
                for (int i = 1; i <= args.length; i += 2) {
                    String targetUrl;
                    if ((i / 2 + 1) < channelInfoList.size()) {
                        ChannelInfo channelInfo = channelInfoList.get(i / 2 + 1);
                        if (!channelInfo.getMediaUrl().equals(moviePlaceHolder)) {
                            MediaProxyTask mediaProxyTask = MediaProxyManager.createProxy("sub_" + (i / 2 + 1), new URI(channelInfo.getMediaUrl()), "flv", null);
                            channelInfo.addProxyTask(mediaProxyTask);
                            targetUrl = String.valueOf(mediaProxyTask.getTargetUrl());
                        } else {
                            targetUrl = moviePlaceHolder;
                        }
                        args[i] = channelInfo.getChannelName();
                    } else {
                        targetUrl = moviePlaceHolder;
                        args[i] = "[空的直播位]";
                    }
                    args[i - 1] = targetUrl.replaceAll(":", "\\\\:");
                }
                Thread.sleep(1000);
                cmdLine = String.format(cmdLine, args);
                long pid = ProcessUtil.createProcess(ffmpegHome + "ffmpeg.exe", cmdLine, false);
                if (pidFile != null) {
                    ProcessUtil.killProcess(Long.parseLong(FilenameUtils.getBaseName(pidFile.getName())));
                    pidFile.delete();
                }
                ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(pidPath + "/" + pid + ".pid"));
                oos.writeObject(channelInfoList);
                oos.close();
                LOGGER.info("ffmpeg转播进程已启动[PID:" + pid + "][" + cmdLine + "]");
            } catch (Throwable e) {
                LOGGER.error("ffmpeg转播进程启动失败", e);
            }
        }
    }

    private boolean cleanupProcess(File pidFile, List<ChannelInfo> channelInfoList) throws IOException {
        long pid = Long.parseLong(FilenameUtils.getBaseName(pidFile.getName()));
        if (!ProcessUtil.isProcessExist(pid)) {
            LOGGER.info("清除无效的pid文件[" + pidFile.getName() + "]");
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(pidFile))) {
                List<ChannelInfo> pidchannelInfoList = (List<ChannelInfo>) ois.readObject();
                channelInfoList.retainAll(pidchannelInfoList);
                cleanupProcess(channelInfoList);
            } catch (ClassNotFoundException e) {
                LOGGER.warn("清理pid[" + pid + "]子进程时发生异常", e);
            }
            return pidFile.delete();
        }
        return false;
    }

    private void cleanupProcess(List<ChannelInfo> channelInfoList) {
        for (ChannelInfo channelInfo : channelInfoList) {
            List<MediaProxyTask> mediaProxyTasks = channelInfo.getMediaProxyTasks();
            if (mediaProxyTasks != null) {
                for (MediaProxyTask mediaProxyTask : mediaProxyTasks) {
                    if (mediaProxyTask instanceof FlvLivingMediaProxyTask) {
                        mediaProxyTask.terminate();
                        mediaProxyTask.waitForTerminate();
                    }
                }
            }
        }
    }
}

