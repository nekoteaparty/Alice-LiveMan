package site.alicemononobe.liveman.mediaproxy;

import lombok.Cleanup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import site.alicemononobe.liveman.mediaproxy.proxytask.FlvLivingMediaProxyTask;
import site.alicemononobe.liveman.mediaproxy.proxytask.MediaProxyTask;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Proxy;
import java.net.URI;

import static site.alicemononobe.liveman.mediaproxy.MediaProxyManager.getTempPath;

@Component
public class FlvLivingMediaProxy implements MediaProxy {
    @Autowired
    private HttpServletResponse response;

    @Override
    public boolean isMatch(URI sourceUrl, String requestFormat) {
        return requestFormat.equals("flv");
    }

    @Override
    public MediaProxyTask createProxyTask(String videoId, URI sourceUrl, Proxy proxy) throws IOException {
        return new FlvLivingMediaProxyTask(videoId, sourceUrl, proxy);
    }

    @Override
    public Object requestHandler(String videoId) throws IOException, InterruptedException {
        File sourceFile = new File(getTempPath() + "/flvLiving/" + videoId + ".flv");
        byte[] buffer = new byte[256 * 1024]; // 256K
        long fileSkipSize = 0;
        long lastWriteTime = System.currentTimeMillis();
        try (ServletOutputStream sos = response.getOutputStream()) {
            while (System.currentTimeMillis() - lastWriteTime < 10000) {
                if (sourceFile.exists()) {
                    if (sourceFile.length() < fileSkipSize) {
                        fileSkipSize = 0;
                    }
                    @Cleanup FileInputStream fis = new FileInputStream(sourceFile);
                    fileSkipSize = fis.skip(fileSkipSize);
                    int readCount = 0;
                    while ((readCount = fis.read(buffer)) > 0) {
                        fileSkipSize += readCount;
                        sos.write(buffer, 0, readCount);
                        lastWriteTime = System.currentTimeMillis();
                    }
                }
                Thread.sleep(300);
            }
        }
        return null;
    }


}

