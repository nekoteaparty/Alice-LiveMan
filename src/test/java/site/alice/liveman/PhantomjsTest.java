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

import org.junit.Test;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

public class PhantomjsTest {
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
                    long dt = System.currentTimeMillis() - lastPaint;
                    lastPaint = System.currentTimeMillis();
                    g.setColor(Color.MAGENTA);
                    g.setFont(new Font("微软雅黑", Font.BOLD, 25));
                    g.drawString("FPS:" + (1000.0 / dt), 20, 20);
                }
            }
        };
        panel.setBackground(new Color(0, 0, 0, 0));
        frame.setAlwaysOnTop(true);
        frame.setUndecorated(true);
        frame.setBackground(new Color(0, 0, 0, 0));
        frame.setContentPane(panel);
        frame.setVisible(true);
        ProcessBuilder processBuilder = new ProcessBuilder();
        frame.setSize(width, height);
        processBuilder.command("phantomjs.exe", "--web-security=false", "F:\\Alice-LiveMan\\capture\\capture.js", "http://localhost:5000/alpha/35399", width + "", height + "");
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
