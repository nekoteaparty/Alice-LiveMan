package site.alicemononobe.liveman.mediaproxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import site.alicemononobe.liveman.mediaproxy.proxytask.M3u8MediaProxyTask;
import site.alicemononobe.liveman.mediaproxy.proxytask.MediaProxyTask;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.Proxy;
import java.net.URI;

@Component
public class M3u8MediaProxy implements MediaProxy {
    @Autowired
    private HttpServletResponse response;

    @Override
    public boolean isMatch(URI url, String requestFormat) {
        return url.getPath().endsWith(".m3u8") && requestFormat.equals("m3u8");
    }

    @Override
    public MediaProxyTask createProxyTask(String videoId, URI sourceUrl, Proxy proxy) {
        return new M3u8MediaProxyTask(videoId, sourceUrl, proxy);
    }

    @Override
    public Object requestHandler(String videoId) {
        try {
            response.sendRedirect("/mediaProxy/temp/m3u8/" + videoId + "/index.m3u8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}

