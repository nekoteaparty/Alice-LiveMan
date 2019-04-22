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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.extern.slf4j.Slf4j;
import site.alice.liveman.customlayout.BlurLayout;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Slf4j
public class ImageSegmentBlurLayout extends BlurLayout {

    public static final String imageHeader = "data:image/png;base64,";

    @JsonIgnore
    private BufferedImage image;

    public Image getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public String getImageBase64() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", bos);
            return imageHeader + Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (Exception e) {
            log.error("encode imageBase64 failed", e);
        }
        return null;
    }

    public void setImageBase64(String imageBase64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(imageBase64.replace(imageHeader, ""));
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            image = ImageIO.read(bis);
        } catch (Exception e) {
            log.error("decode imageBase64 failed", e);
        }
    }

    @Override
    public void paintLayout(Graphics2D g) throws Exception {
        g.drawImage(image, x, y, width, height, null);
    }
}
