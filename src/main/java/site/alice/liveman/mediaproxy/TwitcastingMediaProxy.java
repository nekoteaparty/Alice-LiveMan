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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import site.alice.liveman.mediaproxy.proxytask.MediaProxyTask;
import site.alice.liveman.mediaproxy.proxytask.TwitcastingMediaProxyTask;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.net.Proxy;
import java.net.URI;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Component
public class TwitcastingMediaProxy implements MediaProxy {

    @Autowired
    private HttpServletResponse response;

    @Override
    public boolean isMatch(URI url, String requestFormat) {
        return url.getHost().contains("twitcasting.tv") && url.getScheme().contains("wss") && requestFormat.equals("mp4");
    }

    @Override
    public MediaProxyTask createProxyTask(String videoId, URI sourceUrl, Proxy proxy) {
        return new TwitcastingMediaProxyTask(videoId, sourceUrl, proxy);
    }

    @Override
    public void requestHandler(String videoId) throws Exception {
        MediaProxyTask mediaProxyTask = MediaProxyManager.getExecutedProxyTaskMap().get(videoId);
        if (mediaProxyTask instanceof TwitcastingMediaProxyTask) {
            TwitcastingMediaProxyTask twcProxyTask = (TwitcastingMediaProxyTask) mediaProxyTask;
            BlockingQueue<byte[]> bufferedQueue = new ArrayBlockingQueue<>(20);
            twcProxyTask.addBufferedQueue(bufferedQueue);
            try (ServletOutputStream outputStream = response.getOutputStream()) {
                boolean headerWrote = false;
                while (!twcProxyTask.getTerminated()) {
                    byte[] bytes = bufferedQueue.take();
                    if (!headerWrote) {
                        outputStream.write(twcProxyTask.getM4sHeader());
                        headerWrote = true;
                    }
                    outputStream.write(bytes);
                }
            } finally {
                twcProxyTask.removeBufferedQueue(bufferedQueue);
            }
        }
    }
}

