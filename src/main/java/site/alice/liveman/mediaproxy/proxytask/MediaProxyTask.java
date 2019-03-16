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
package site.alice.liveman.mediaproxy.proxytask;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskTimeoutException;
import site.alice.liveman.mediaproxy.MediaProxyManager;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.utils.FfmpegUtil;
import site.alice.liveman.utils.ProcessUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.Serializable;
import java.net.Proxy;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@Slf4j
public abstract class MediaProxyTask implements Runnable, Serializable {

    @Autowired
    private           LiveManSetting       liveManSetting;
    private           String               videoId;
    private           URI                  sourceUrl;
    private           URI                  targetUrl;
    private           VideoInfo            videoInfo;
    private           List<MediaProxyTask> parentProxyTasks;
    private transient Thread               runThread;
    private volatile  Boolean              isTerminated;

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public URI getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(URI sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public URI getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(URI targetUrl) {
        this.targetUrl = targetUrl;
    }

    public VideoInfo getVideoInfo() {
        return videoInfo;
    }

    public void setVideoInfo(VideoInfo videoInfo) {
        this.videoInfo = videoInfo;
    }

    public Boolean getTerminated() {
        return isTerminated;
    }

    public BufferedImage getKeyFrame() {
        String fileName = UUID.randomUUID() + ".jpg";
        String keyFrameCmdLine = FfmpegUtil.buildKeyFrameCmdLine(targetUrl.toString(), fileName);
        long process = ProcessUtil.createProcess(keyFrameCmdLine, getVideoId() + "_KeyFrame");
        try {
            if (ProcessUtil.waitProcess(process, 10000)) {
                return ImageIO.read(new File(fileName));
            } else {
                ProcessUtil.killProcess(process);
                log.error("获取[" + targetUrl + "]关键帧超时");
            }
        } catch (Throwable t) {
            log.error("获取[" + targetUrl + "]关键帧失败", t);
        } finally {
            new File(fileName).delete();
        }
        return null;
    }

    protected MediaProxyTask(String videoId, URI sourceUrl) {
        this.videoId = videoId;
        this.sourceUrl = sourceUrl;
    }

    @Override
    public synchronized void run() {
        isTerminated = false;
        try {
            runThread = Thread.currentThread();
            log.info(getVideoId() + "代理任务启动@" + runThread.getName());
            runTask();
        } catch (Throwable e) {
            log.error(getVideoId() + "代理任务异常退出", e);
        } finally {
            isTerminated = true;
            terminateTask();
            MediaProxyManager.removeProxy(this);
            // 将自身从代理列表中移除
            if (parentProxyTasks != null) {
                parentProxyTasks.remove(this);
            }
            log.info(getVideoId() + "代理任务终止@" + runThread.getName());
        }
    }

    public void terminate() {
        log.info("开始终止" + getVideoId() + "代理任务@" + runThread.getName());
        isTerminated = true;
    }

    public synchronized void waitForTerminate() {
    }

    protected abstract void runTask() throws Exception;

    protected abstract void terminateTask();

    protected Thread getRunThread() {
        return runThread;
    }

    public List<MediaProxyTask> getParentProxyTasks() {
        return parentProxyTasks;
    }

    public void setParentProxyTasks(List<MediaProxyTask> parentProxyTasks) {
        this.parentProxyTasks = parentProxyTasks;
    }
}
