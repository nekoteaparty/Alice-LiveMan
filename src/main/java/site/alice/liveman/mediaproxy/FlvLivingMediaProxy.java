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
package site.alice.liveman.mediaproxy;

import lombok.Cleanup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import site.alice.liveman.mediaproxy.proxytask.FlvLivingMediaProxyTask;
import site.alice.liveman.mediaproxy.proxytask.MediaProxyTask;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Proxy;
import java.net.URI;

import static site.alice.liveman.mediaproxy.MediaProxyManager.getTempPath;

@Component
public class FlvLivingMediaProxy implements MediaProxy {
    @Autowired
    private HttpServletResponse response;

    @Override
    public boolean isMatch(URI sourceUrl, String requestFormat) {
        return requestFormat.equals("flv");
    }

    @Override
    public MediaProxyTask createProxyTask(String videoId, URI sourceUrl) throws IOException {
        return new FlvLivingMediaProxyTask(videoId, sourceUrl);
    }

    @Override
    public void requestHandler(String videoId) throws IOException, InterruptedException {
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
    }


}

