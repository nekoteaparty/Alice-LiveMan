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

import com.baidu.aip.client.BaseClient;
import com.baidu.aip.http.AipRequest;
import com.baidu.aip.http.EBodyFormat;
import com.baidu.aip.util.Base64Util;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.alice.liveman.bo.ExternalAppSecretBO;
import site.alice.liveman.dataobject.ExternalAppSecretDO;
import site.alice.liveman.service.external.ExternalServiceType;
import site.alice.liveman.service.external.TextLocation;
import site.alice.liveman.service.external.TextLocationService;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

@Slf4j
@Service
public class AliceCommentTextLocationService implements TextLocationService {

    @Autowired
    private ExternalAppSecretBO externalAppSecretBO;

    @Override
    public void requireTextLocation(BufferedImage image, BiConsumer<List<TextLocation>, BufferedImage> callback) {
        try {
            ExternalAppSecretDO ocrAppSecret = externalAppSecretBO.getAppSecret(ExternalServiceType.BAIDU_API);
            EDLAliceAipClient client = new EDLAliceAipClient(ocrAppSecret.getAppId(), ocrAppSecret.getAppKey(), ocrAppSecret.getSecretKey());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", bos);
            JSONObject verifyResult = client.aliceComment(bos.toByteArray());
            if (!verifyResult.isNull("results")) {
                List<TextLocation> textLocations = new ArrayList<>();
                for (Object o : verifyResult.getJSONArray("results")) {
                    JSONObject result = (JSONObject) o;
                    JSONObject commentLocation = result.getJSONObject("location");
                    TextLocation textLocation = new TextLocation();
                    textLocation.setRectangle(new Rectangle(commentLocation.getInt("left"), commentLocation.getInt("top"), commentLocation.getInt("width"), commentLocation.getInt("height")));
                    textLocation.setScore(result.getDouble("score"));
                    textLocations.add(textLocation);
                }
                callback.accept(textLocations, image);
            } else {
                log.error("请求EastDL接口失败:" + verifyResult);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    class EDLAliceAipClient extends BaseClient {

        public static final String ALICE_COMMENT_URL = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/detection/alice_comment";

        protected EDLAliceAipClient(String appId, String apiKey, String secretKey) {
            super(appId, apiKey, secretKey);
        }

        public JSONObject aliceComment(byte[] image) {
            AipRequest request = new AipRequest();
            preOperation(request);
            request.getHeaders().put("Content-Type", "application/json");
            String base64Content = Base64Util.encode(image);
            request.addBody("image", base64Content);
            request.setUri(ALICE_COMMENT_URL);
            request.setBodyFormat(EBodyFormat.RAW_JSON);
            postOperation(request);
            return requestServer(request);
        }

    }
}
