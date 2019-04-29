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
import site.alice.liveman.customlayout.BlurLayout;
import site.alice.liveman.customlayout.CustomLayout;
import site.alice.liveman.customlayout.impl.BrowserLayout;
import site.alice.liveman.customlayout.impl.ImageSegmentBlurLayout;
import site.alice.liveman.customlayout.impl.RectangleBlurLayout;
import site.alice.liveman.jenum.VideoBannedTypeEnum;
import site.alice.liveman.mediaproxy.MediaProxyManager;
import site.alice.liveman.mediaproxy.proxytask.MediaProxyTask;
import site.alice.liveman.model.VideoCropConf;
import site.alice.liveman.model.VideoInfo;

import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
        String resolution = videoInfo.getResolution();
        if (resolution == null) {
            BufferedImage keyFrame = mediaProxyTask.getKeyFrame();
            if (keyFrame != null) {
                resolution = keyFrame.getWidth() + "x" + keyFrame.getHeight();
                videoInfo.setResolution(resolution);
                log.info("媒体分辨率[videoId=" + videoId + "]获取成功！[" + resolution + "]");
            } else {
                log.warn("媒体分辨率[videoId=" + videoId + "]获取失败！");
                return;
            }
        }
        int[] sizes = Arrays.stream(resolution.split("x")).mapToInt(Integer::parseInt).toArray();
        VideoCropConf cropConf = videoInfo.getCropConf();
        if (cropConf.getVideoBannedType() == VideoBannedTypeEnum.CUSTOM_SCREEN) {
            byte[] cachedDrawBytes = cropConf.getCachedDrawBytes();
            if (cachedDrawBytes == null) {
                boolean canCache = true;
                BufferedImage image = new BufferedImage((int) (sizes[0] * (720.0 / sizes[1])), 720, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                graphics.setBackground(new Color(0, 0, 0, 0));
                List<CustomLayout> customLayoutList = cropConf.getLayouts();
                if (CollectionUtils.isNotEmpty(customLayoutList)) {
                    for (CustomLayout customLayout : customLayoutList) {
                        if (customLayout instanceof BlurLayout) {
                            continue;
                        }
                        if (customLayout instanceof BrowserLayout) {
                            canCache = false;
                        }
                        try {
                            customLayout.paintLayout(graphics);
                        } catch (Exception e) {
                            log.error(customLayout.getClass().getName() + "[videoId=" + videoInfo.getVideoId() + "]渲染出错", e);
                        }
                    }
                }
                if (sizes[1] != 720) {
                    BufferedImage originalSizeImage = new BufferedImage(sizes[0], sizes[1], BufferedImage.TYPE_INT_ARGB);
                    Graphics2D originalSizeImageGraphics = originalSizeImage.createGraphics();
                    originalSizeImageGraphics.drawImage(image.getScaledInstance(sizes[0], sizes[1], Image.SCALE_SMOOTH), 0, 0, null);
                    image = originalSizeImage;
                }
                PngEncoderB pngEncoderB = new PngEncoderB();
                pngEncoderB.setCompressionLevel(3);
                pngEncoderB.setEncodeAlpha(true);
                pngEncoderB.setImage(image);
                cachedDrawBytes = pngEncoderB.pngEncode();
                if (canCache) {
                    cropConf.setCachedDrawBytes(cachedDrawBytes);
                }
            }
            try (OutputStream os = response.getOutputStream()) {
                os.write(cachedDrawBytes);
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
        String resolution = videoInfo.getResolution();
        if (resolution == null) {
            log.info("未知媒体分辨率[videoId=" + videoId + "]，尝试获取...");
            BufferedImage keyFrame = mediaProxyTask.getKeyFrame();
            if (keyFrame != null) {
                resolution = keyFrame.getWidth() + "x" + keyFrame.getHeight();
                videoInfo.setResolution(resolution);
                log.info("媒体分辨率[videoId=" + videoId + "]获取成功！[" + resolution + "]");
            } else {
                log.warn("媒体分辨率[videoId=" + videoId + "]获取失败！");
                return;
            }
        }
        int[] sizes = Arrays.stream(resolution.split("x")).mapToInt(Integer::parseInt).toArray();
        VideoCropConf cropConf = videoInfo.getCropConf();
        if (cropConf.getVideoBannedType() == VideoBannedTypeEnum.CUSTOM_SCREEN) {
            try (OutputStream os = response.getOutputStream()) {
                byte[] cachedBlurBytes = cropConf.getCachedBlurBytes();
                if (cachedBlurBytes == null) {
                    BufferedImage image = new BufferedImage((int) (sizes[0] * (720.0 / sizes[1])), 720, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D graphics = image.createGraphics();
                    List<CustomLayout> customLayoutList = cropConf.getLayouts();
                    for (CustomLayout customLayout : customLayoutList) {
                        if (customLayout instanceof RectangleBlurLayout) {
                            try {
                                customLayout.paintLayout(graphics);
                            } catch (Exception e) {
                                log.error(customLayout.getClass().getName() + "[videoId=" + videoInfo.getVideoId() + "]渲染出错", e);
                            }
                        }
                    }
                    for (CustomLayout customLayout : customLayoutList) {
                        if (customLayout instanceof ImageSegmentBlurLayout) {
                            try {
                                customLayout.paintLayout(graphics);
                            } catch (Exception e) {
                                log.error(customLayout.getClass().getName() + "[videoId=" + videoInfo.getVideoId() + "]渲染出错", e);
                            }
                        }
                    }
                    if (sizes[1] != 720) {
                        BufferedImage originalSizeImage = new BufferedImage(sizes[0], sizes[1], BufferedImage.TYPE_INT_ARGB);
                        originalSizeImage.createGraphics().drawImage(image, 0, 0, sizes[0], sizes[1], null);
                        image = originalSizeImage;
                    }
                    PngEncoderB pngEncoderB = new PngEncoderB();
                    pngEncoderB.setCompressionLevel(3);
                    pngEncoderB.setEncodeAlpha(true);
                    pngEncoderB.setImage(image);
                    cachedBlurBytes = pngEncoderB.pngEncode();
                    cropConf.setCachedBlurBytes(cachedBlurBytes);
                }
                os.write(cachedBlurBytes);
                os.flush();
            } catch (Exception e) {
                log.error("无法输出图像数据到响应流[videoId=" + videoInfo.getVideoId() + "]", e);
            }
        }
    }
}
