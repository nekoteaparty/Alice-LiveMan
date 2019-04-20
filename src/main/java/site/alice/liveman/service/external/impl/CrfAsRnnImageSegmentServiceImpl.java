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

package site.alice.liveman.service.external.impl;

import com.keypoint.PngEncoder;
import com.keypoint.PngEncoderB;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import site.alice.liveman.service.external.ImageSegmentService;
import site.alice.liveman.service.external.consumer.ImageSegmentConsumer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
public class CrfAsRnnImageSegmentServiceImpl implements ImageSegmentService {
    public static final String imageHeader = "data:image/png;base64,";

    @Override
    public void imageSegment(BufferedImage image, ImageSegmentConsumer consumer) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", bos);
            Document document = Jsoup.connect(
                    "http://www.robots.ox.ac.uk/~szheng/crfasrnndemo/classify_upload")
                    .header("Accept-Encoding", "gzip, deflate").maxBodySize(0).timeout(30000)
                    .userAgent(
                            "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.103 Safari/537.36").data("imagefile", "fakename.jpg", new ByteArrayInputStream(bos.toByteArray())).post();
            Elements elements = document.select("img.img-responsive");
            List<BufferedImage> images = new ArrayList<>();
            for (Element element : elements) {
                String imgSrc = element.attr("src");
                if (imgSrc != null && imgSrc.startsWith(imageHeader)) {
                    String imageBase64 = imgSrc.replace(imageHeader, "");
                    images.add(ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(imageBase64))));
                }
            }
            BufferedImage originalImage = images.get(0);
            BufferedImage filterImage = images.get(1);
            int alpha = 180;
            BufferedImage sameRangeImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = originalImage.createGraphics();
            graphics.setColor(new Color(192, 128, 128, alpha));
            graphics.fillRect(0, 0, originalImage.getWidth(), originalImage.getHeight());
            Raster originalImageData = originalImage.getData();
            Raster filterImageData = filterImage.getData();
            int[] odata = new int[4];
            int[] fdata = new int[4];
            for (int i = 0; i < originalImage.getWidth(); i++) {
                for (int j = 0; j < originalImage.getHeight(); j++) {
                    if (Arrays.equals(originalImageData.getPixel(i, j, odata), filterImageData.getPixel(i, j, fdata))) {
                        sameRangeImage.setRGB(i, j, Color.BLACK.getRGB());
                    }
                }
            }
            consumer.accept(sameRangeImage, image);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
