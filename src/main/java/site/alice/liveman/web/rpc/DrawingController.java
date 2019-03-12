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

package site.alice.liveman.web.rpc;

import com.keypoint.PngEncoderB;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import site.alice.liveman.customlayout.CustomLayout;
import site.alice.liveman.customlayout.impl.BlurLayout;
import site.alice.liveman.jenum.VideoBannedTypeEnum;
import site.alice.liveman.mediaproxy.MediaProxyManager;
import site.alice.liveman.mediaproxy.proxytask.MediaProxyTask;
import site.alice.liveman.model.VideoInfo;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

@Slf4j
@Controller
@RequestMapping("/api/drawing")
public class DrawingController {
    @Autowired
    private HttpServletResponse response;

    @RequestMapping(method = RequestMethod.GET, value = "/screen/{videoId}")
    public void screen(@PathVariable("videoId") String videoId) {
        Map<String, MediaProxyTask> executedProxyTaskMap = MediaProxyManager.getExecutedProxyTaskMap();
        MediaProxyTask mediaProxyTask = executedProxyTaskMap.get(videoId);
        if (mediaProxyTask == null) {
            log.info("找不到请求的媒体代理任务信息[videoId=" + videoId + "]");
            return;
        }
        VideoInfo videoInfo = mediaProxyTask.getVideoInfo();
        if (videoInfo == null) {
            log.info("找不到请求的媒体信息[videoId=" + videoId + "]");
            return;
        }
        if (videoInfo.getCropConf().getVideoBannedType() == VideoBannedTypeEnum.CUSTOM_SCREEN) {
            BufferedImage image = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setBackground(new Color(0, 0, 0, 0));
            Set<CustomLayout> customLayoutList = videoInfo.getCropConf().getLayouts();
            if (CollectionUtils.isNotEmpty(customLayoutList)) {
                for (CustomLayout customLayout : customLayoutList) {
                    if (customLayout instanceof BlurLayout) {
                        continue;
                    }
                    try {
                        customLayout.paintLayout(graphics);
                    } catch (Exception e) {
                        log.error(customLayout.getClass().getName() + "[videoId=" + videoInfo.getVideoId() + "]渲染出错", e);
                    }
                }
            }
            try (OutputStream os = response.getOutputStream()) {
                PngEncoderB pngEncoderB = new PngEncoderB();
                pngEncoderB.setCompressionLevel(6);
                pngEncoderB.setEncodeAlpha(true);
                pngEncoderB.setImage(image);
                os.write(pngEncoderB.pngEncode());
                os.flush();
            } catch (Exception e) {
                log.error("无法输出图像数据到响应流[videoId=" + videoInfo.getVideoId() + "]", e);
            }
        }
    }


    @RequestMapping(method = RequestMethod.GET, value = "/mask/{videoId}")
    public void mask(@PathVariable("videoId") String videoId) {
        Map<String, MediaProxyTask> executedProxyTaskMap = MediaProxyManager.getExecutedProxyTaskMap();
        MediaProxyTask mediaProxyTask = executedProxyTaskMap.get(videoId);
        if (mediaProxyTask == null) {
            log.info("找不到请求的媒体代理任务信息[videoId=" + videoId + "]");
            return;
        }
        VideoInfo videoInfo = mediaProxyTask.getVideoInfo();
        if (videoInfo == null) {
            log.info("找不到请求的媒体信息[videoId=" + videoId + "]");
            return;
        }
        if (videoInfo.getCropConf().getVideoBannedType() == VideoBannedTypeEnum.CUSTOM_SCREEN) {
            BufferedImage image = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            Set<CustomLayout> customLayoutList = videoInfo.getCropConf().getLayouts();
            for (CustomLayout customLayout : customLayoutList) {
                if (customLayout instanceof BlurLayout) {
                    try {
                        customLayout.paintLayout(graphics);
                    } catch (Exception e) {
                        log.error(customLayout.getClass().getName() + "[videoId=" + videoInfo.getVideoId() + "]渲染出错", e);
                    }
                }
            }
            try (OutputStream os = response.getOutputStream()) {
                ImageIO.write(image, "jpg", os);
            } catch (Exception e) {
                log.error("无法输出图像数据到响应流[videoId=" + videoInfo.getVideoId() + "]", e);
            }
        }
    }
}
