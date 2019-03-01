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

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

@Slf4j
public class ImageLayout extends CustomLayout {

    private String src;
    private float  opacity;
    private String imageBase64;

    @Override
    public void paintLayout(Graphics2D g) throws IOException {
        if (imageBase64 == null) {
            int startPoint = src.indexOf(",");
            if (startPoint > -1) {
                imageBase64 = src.substring(startPoint + 1);
            } else {
                imageBase64 = src;
            }
        }
        byte[] bytes = Base64.getDecoder().decode(imageBase64);
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
            BufferedImage image = ImageIO.read(bis);
            Composite oldComp = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            g.drawImage(image, x, y, width, height, null);
            g.setComposite(oldComp);
        }
    }

    public float getOpacity() {
        return opacity;
    }

    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }
}
