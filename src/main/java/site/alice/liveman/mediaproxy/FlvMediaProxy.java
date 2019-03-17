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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import site.alice.liveman.mediaproxy.proxytask.FlvMediaProxyTask;
import site.alice.liveman.mediaproxy.proxytask.MediaProxyTask;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;

@Slf4j
@Component
public class FlvMediaProxy implements MediaProxy {
    @Autowired
    private HttpServletResponse response;

    @Override
    public boolean isMatch(URI url, String requestFormat) {
        return requestFormat.equals("flv");
    }

    @Override
    public MediaProxyTask createProxyTask(String videoId, URI sourceUrl) throws IOException {
        return new FlvMediaProxyTask(videoId, sourceUrl);
    }

    @Override
    public void requestHandler(String videoId) throws Exception {
        MediaProxyTask mediaProxyTask = MediaProxyManager.getExecutedProxyTaskMap().get(videoId);
        if (mediaProxyTask != null) {
            File flvFilePath = new File(MediaProxyManager.getTempPath() + "/flv/" + videoId + "/");
            File[] flvFiles = flvFilePath.listFiles();
            if (flvFiles != null && flvFiles.length > 0) {
                Arrays.sort(flvFiles, new Comparator<File>() {
                    @Override
                    public int compare(File o1, File o2) {
                        return (int) (o2.lastModified() - o1.lastModified());
                    }
                });
                byte[] buffer = new byte[1024 * 1024];
                try (RandomAccessFile raf = new RandomAccessFile(flvFiles[0], "r")) {
                    try (OutputStream os = response.getOutputStream()) {
                        if (raf.length() > 2 * buffer.length) {
                            // 读FLV头部1KB数据
                            raf.read(buffer, 0, 1024);
                            os.write(buffer, 0, 1024);
                            raf.seek(raf.length() - buffer.length);
                        }
                        while (!mediaProxyTask.getTerminated()) {
                            int readed = 0;
                            if ((readed = raf.read(buffer)) < 0) {
                                log.info("not have any stream bytes[videoId=" + videoId + "]:" + raf.length());
                                Thread.sleep(500);
                            } else {
                                log.info("read stream bytes[videoId=" + videoId + "]:" + raf.getFilePointer() + "@" + raf.length() + ", read:" + readed);
                                os.write(buffer, 0, readed);
                                os.flush();
                            }
                        }
                    }
                }
            }
        }
    }
}
