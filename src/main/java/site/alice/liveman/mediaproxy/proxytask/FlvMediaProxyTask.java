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
import org.apache.http.client.methods.HttpGet;
import site.alice.liveman.mediaproxy.MediaProxyManager;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.utils.HttpRequestUtil;

import java.io.File;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class FlvMediaProxyTask extends MediaProxyTask {
    private static final int           MAX_RETRY_COUNT = 30;
    private              HttpGet       httpGet;
    protected            AtomicInteger retryCount      = new AtomicInteger(0);

    public FlvMediaProxyTask(String videoId, URI sourceUrl) {
        super(videoId, sourceUrl);
    }

    @Override
    protected void runTask() throws Exception {
        while (!getTerminated() && retryCount.get() < MAX_RETRY_COUNT) {
            try {
                File flvFile = new File(getTempPath() + "/" + System.currentTimeMillis() + ".flv");
                httpGet = new HttpGet(getSourceUrl());
                HttpRequestUtil.downloadToFile(httpGet, flvFile);
            } catch (Throwable t) {
                if (getTerminated()) {
                    return;
                }
                log.error(getVideoId() + "出错重试(" + retryCount.incrementAndGet() + "/" + MAX_RETRY_COUNT + ")次", t);
            }
            Thread.sleep(1000);
        }
    }

    @Override
    public String getTempPath() {
        return MediaProxyManager.getTempPath() + "/flv/" + getVideoInfo().getVideoUnionId();
    }

    @Override
    protected void beforeTerminate() {
        if (httpGet != null) {
            httpGet.abort();
        }
    }
}
