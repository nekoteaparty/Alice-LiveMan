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

package site.alice.liveman;

import com.keypoint.PngEncoderB;
import org.aspectj.util.Reflection;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

public class PhantomjsTest {

    @Test
    public void pipeTest() throws IOException, InterruptedException {
        int fps = 5;
        int width = 500;
        int height = 500;
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("f:/ffmpeg", "-i", "F:\\循环的歌姬_720p.flv", "-framerate", fps + "", "-f", "image2pipe", "-i", "pipe:0", "-filter_complex", "[0:v]fps=30,scale=1280x720[outv0];[outv0][1:v]overlay=0:0",
                "-vcodec", "h264", "-preset", "ultrafast", "-f", "flv", "rtmp://txy.live-send.acg.tv/live-txy/?streamname=live_498498_8678481&key=5669514a2bc955b432ba5f7ab9936a77");
        processBuilder.redirectError(new File("ffmpeg.out"));
        Process start = processBuilder.start();
        OutputStream outputStream = start.getOutputStream();
        BufferedImage 卡缇娅 = ImageIO.read(new URL("http://img3.jiemian.com/jiemian/original/20181010/153918284935857600_a580xH.jpg"));
        BufferedImage image = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setBackground(new Color(0, 0, 0, 0));
        Font font = new Font("微软雅黑", Font.PLAIN, 28);
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        ProcessBuilder phantomjs = new ProcessBuilder();
        phantomjs.command("phantomjs.exe", "--web-security=false", "F:\\Alice-LiveMan\\capture\\capture.js", "https://nekosunflower.github.io/BiliChat/?id=36577", width + "", height + "");
        Process phantomjsProcess = phantomjs.start();
        BufferedInputStream is = (BufferedInputStream) phantomjsProcess.getInputStream();
        Scanner sc = new Scanner(is);
        AtomicReference<BufferedImage> imageRef = new AtomicReference<>();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (sc.hasNextLine()) {
                    String nextLine = sc.nextLine();
                    if (nextLine.startsWith("capture:")) {
                        nextLine = nextLine.substring("capture:".length());
                        byte[] captureData = Base64.getDecoder().decode(nextLine);
                        ByteArrayInputStream bis = new ByteArrayInputStream(captureData);
                        try {
                            imageRef.set(ImageIO.read(bis));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println(nextLine);
                    }
                }
            }
        });
        t.start();
        int di = 1, i = 0;

        while (true) {
            if (i == 0) {
                di = 1;
            }
            if (i == 255) {
                di = -1;
            }
            i += di;
            long startTime = System.nanoTime();
            graphics.clearRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(new Color(i, 255 - i, i / 2, i));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(new Color(255 - i, i, 255 - i / 2));
            graphics.setFont(font);
            graphics.drawImage(卡缇娅, 300 - i, 100 + i, null);
            graphics.drawString(dateFormat.format(new Date()), (int) (i * (1280 / 255.0)), (int) (i * (720 / 255.0)));
            if (imageRef.get() != null) {
                graphics.drawImage(imageRef.get(), 0, 0, null);
            }
            PngEncoderB pngEncoderB = new PngEncoderB();
            pngEncoderB.setImage(image);
            pngEncoderB.setEncodeAlpha(true);
            outputStream.write(pngEncoderB.pngEncode());
            outputStream.flush();
            long dt = (System.nanoTime() - startTime) / 1000000;
            Thread.sleep(Math.max(0, (1000 / fps) - dt));
        }
    }

    @Test
    public void capturePhantomjs() throws IOException {
        AtomicReference<BufferedImage> imageRef = new AtomicReference<>();

        int width = 500;
        int height = 500;
        JFrame frame = new JFrame();
        JPanel panel = new JPanel() {

            private Long lastPaint = System.currentTimeMillis();

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                BufferedImage image = imageRef.get();
                if (image != null) {
                    g.drawImage(image, 0, 0, null);
                }
            }
        };
        panel.setBackground(new Color(0, 0, 0, 0));
        frame.setAlwaysOnTop(true);
        frame.setUndecorated(true);
        frame.setBackground(new Color(0, 0, 0, 0));
        frame.setContentPane(panel);
        frame.setVisible(true);
        frame.setSize(width, height);
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("phantomjs.exe", "--web-security=false", "F:\\Alice-LiveMan\\capture\\capture.js", "http://localhost:5000/alpha/102", width + "", height + "");
        Process process = processBuilder.start();
        Scanner sc = new Scanner(process.getInputStream());
        while (sc.hasNextLine()) {
            String nextLine = sc.nextLine();
            if (nextLine.startsWith("capture:")) {
                nextLine = nextLine.substring("capture:".length());
                byte[] captureData = Base64.getDecoder().decode(nextLine);
                ByteArrayInputStream bis = new ByteArrayInputStream(captureData);
                imageRef.set(ImageIO.read(bis));
                frame.repaint();
            } else {
                System.out.println(nextLine);
            }
        }
    }
}
