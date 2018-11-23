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
package site.alice.liveman.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;

public class DynamicAreaUtil {

    public static File createAreaImage(String context, File imageFile, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setFont(new Font(null, Font.PLAIN, 28));
        g.setBackground(Color.BLACK);
        int x = 0;
        int y = 0;
        for (String line : context.split("\n")) {
            g.drawString(line, x, y += g.getFontMetrics().getHeight());
        }
        ImageIO.write(image, "png", imageFile);
        return imageFile;
    }

}
