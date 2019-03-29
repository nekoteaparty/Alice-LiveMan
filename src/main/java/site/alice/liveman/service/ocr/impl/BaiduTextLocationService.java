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

package site.alice.liveman.service.ocr.impl;

import com.baidu.aip.ocr.AipOcr;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.alice.liveman.bo.OcrAppSecretBO;
import site.alice.liveman.dataobject.OcrAppSecretDO;
import site.alice.liveman.service.ocr.TextLocation;
import site.alice.liveman.service.ocr.TextLocationService;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;

@Slf4j
@Service
public class BaiduTextLocationService implements TextLocationService {

    @Autowired
    private OcrAppSecretBO ocrAppSecretBO;

    @Override
    public void requireTextLocation(BufferedImage image, BiConsumer<List<TextLocation>, BufferedImage> callback) {
        try {
            OcrAppSecretDO ocrAppSecret = ocrAppSecretBO.getOcrAppSecret("baidu");
            AipOcr client = new AipOcr(ocrAppSecret.getAppId(), ocrAppSecret.getAppKey(), ocrAppSecret.getSecretKey());
            HashMap<String, String> options = new HashMap<>();
            options.put("language_type", "JAP");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", bos);
            JSONObject general = client.general(bos.toByteArray(), options);
            callback.accept(parseTextLocationList(general), image);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<TextLocation> parseTextLocationList(JSONObject jsonObject) {
        List<TextLocation> textLocations = new ArrayList<>();
        JSONArray wordsResult = jsonObject.getJSONArray("words_result");
        for (int i = 0; i < wordsResult.length(); i++) {
            JSONObject wordResult = wordsResult.getJSONObject(i);
            TextLocation textLocation = new TextLocation();
            JSONObject location = wordResult.getJSONObject("location");
            textLocation.setRectangle(new Rectangle(location.getInt("left"), location.getInt("top"), location.getInt("width"), location.getInt("height")));
            textLocation.setText(wordResult.getString("words"));
            textLocations.add(textLocation);
        }
        return textLocations;
    }

}
