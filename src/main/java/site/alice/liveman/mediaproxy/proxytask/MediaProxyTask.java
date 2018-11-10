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
package site.alice.liveman.mediaproxy.proxytask;

import lombok.extern.slf4j.Slf4j;
import site.alice.liveman.mediaproxy.MediaProxyManager;
import site.alice.liveman.model.VideoInfo;

import java.io.Serializable;
import java.net.Proxy;
import java.net.URI;
import java.util.List;

@Slf4j
public abstract class MediaProxyTask implements Runnable, Serializable {

    private           String               videoId;
    private           URI                  sourceUrl;
    private           URI                  targetUrl;
    private           VideoInfo            videoInfo;
    private           List<MediaProxyTask> parentProxyTasks;
    private transient Proxy                proxy;
    private transient Thread               runThread;
    private volatile  Boolean              isTerminated;

    public Proxy getProxy() {
        return proxy;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

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

    public MediaProxyTask(String videoId, URI sourceUrl, Proxy proxy) {
        this.videoId = videoId;
        this.sourceUrl = sourceUrl;
        this.proxy = proxy;
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
            log.info(getVideoId() + "代理任务终止@" + runThread.getName());
            MediaProxyManager.removeProxy(this);
            // 将自身从代理列表中移除
            if (parentProxyTasks != null) {
                parentProxyTasks.remove(this);
            }
            terminateTask();
        }
    }

    public void terminate() {
        isTerminated = true;
    }

    public synchronized void waitForTerminate() {
    }

    protected abstract void runTask() throws Exception;

    protected abstract void terminateTask();

    public List<MediaProxyTask> getParentProxyTasks() {
        return parentProxyTasks;
    }

    public void setParentProxyTasks(List<MediaProxyTask> parentProxyTasks) {
        this.parentProxyTasks = parentProxyTasks;
    }
}
