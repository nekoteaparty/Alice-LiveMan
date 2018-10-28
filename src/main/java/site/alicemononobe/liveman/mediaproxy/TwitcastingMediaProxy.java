package site.alicemononobe.liveman.mediaproxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import site.alicemononobe.liveman.mediaproxy.proxytask.MediaProxyTask;
import site.alicemononobe.liveman.mediaproxy.proxytask.TwitcastingMediaProxyTask;

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
    public Object requestHandler(String videoId) throws Exception {
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
        return null;
    }
}

