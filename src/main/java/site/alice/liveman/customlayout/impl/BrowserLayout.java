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

package site.alice.liveman.customlayout.impl;

import lombok.extern.slf4j.Slf4j;
import site.alice.liveman.customlayout.CustomLayout;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.utils.ProcessUtil;
import site.alice.liveman.utils.ThreadPoolUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class BrowserLayout extends CustomLayout {

    private AtomicReference<BufferedImage> imageRef = new AtomicReference<>();
    private URL                            url;
    private long                           lastPaint;
    private long                           pid;

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    @Override
    public void paintLayout(Graphics2D g) {
        lastPaint = System.nanoTime();
        ProcessUtil.AliceProcess browserProcess = ProcessUtil.getAliceProcess(pid);
        if (browserProcess == null || !browserProcess.isAlive()) {
            log.info("BrowserLayout[videoId=" + videoInfo.getVideoId() + "]启动Browser...");
            String[] args = new String[]{new File("phantomjs").getAbsolutePath(), "--web-security=false",
                                         new File("capture.js").getAbsolutePath(), getUrl().toString(),
                                         width + "", height + ""};
            pid = ProcessUtil.createProcess(args);
            final ProcessUtil.AliceProcess _browserProcess = ProcessUtil.getAliceProcess(pid);
            if (_browserProcess != null && _browserProcess.isAlive()) {
                ThreadPoolUtil.execute(new Runnable() {
                    @Override
                    public void run() {
                        BufferedInputStream is = (BufferedInputStream) _browserProcess.getInputStream();
                        Scanner sc = new Scanner(is);
                        while (sc.hasNextLine()) {
                            String nextLine = sc.nextLine();
                            if (nextLine.startsWith("capture:")) {
                                nextLine = nextLine.substring("capture:".length());
                                byte[] captureData = Base64.getDecoder().decode(nextLine);
                                ByteArrayInputStream bis = new ByteArrayInputStream(captureData);
                                try {
                                    imageRef.set(ImageIO.read(bis));
                                } catch (IOException e) {
                                    log.error("BrowserLayout[videoId=" + videoInfo.getVideoId() + "]", e);
                                }
                            } else {
                                log.info("BrowserLayout[videoId=" + videoInfo.getVideoId() + "]:" + nextLine);
                            }
                            if (TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - lastPaint) > 10) {
                                log.info("BrowserLayout[videoId=" + videoInfo.getVideoId() + "]超过10秒闲置，自动释放Browser资源");
                                ProcessUtil.killProcess(ProcessUtil.getProcessHandle(_browserProcess));
                                return;
                            }
                        }
                    }
                });
            }
        }
        BufferedImage image = imageRef.get();
        if (image != null) {
            g.drawImage(image, x, y, image.getWidth(), image.getHeight(), null);
        }
    }
}
