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

package site.alice.liveman.service.external.consumer.impl;

import lombok.extern.slf4j.Slf4j;
import site.alice.liveman.customlayout.CustomLayout;
import site.alice.liveman.customlayout.impl.ImageSegmentBlurLayout;
import site.alice.liveman.mediaproxy.MediaProxyManager;
import site.alice.liveman.mediaproxy.proxytask.MediaProxyTask;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.service.external.consumer.ImageSegmentConsumer;

import java.awt.image.BufferedImage;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class ImageSegmentConsumerImpl implements ImageSegmentConsumer {

    private VideoInfo videoInfo;

    public ImageSegmentConsumerImpl(VideoInfo videoInfo) {
        this.videoInfo = videoInfo;
    }

    @Override
    public void accept(BufferedImage resultImage, BufferedImage originalImage) {
        try {
            double scale = 720.0 / originalImage.getHeight();
            CopyOnWriteArrayList<CustomLayout> customLayouts = videoInfo.getCropConf().getLayouts();
            customLayouts.removeIf(customLayout -> customLayout instanceof ImageSegmentBlurLayout);
            ImageSegmentBlurLayout imageSegmentBlurLayout = new ImageSegmentBlurLayout();
            imageSegmentBlurLayout.setIndex(10);
            imageSegmentBlurLayout.setImage(resultImage);
            imageSegmentBlurLayout.setVideoInfo(videoInfo);
            imageSegmentBlurLayout.setX(0);
            imageSegmentBlurLayout.setY(0);
            imageSegmentBlurLayout.setWidth((int) (originalImage.getWidth() * scale));
            imageSegmentBlurLayout.setHeight((int) (originalImage.getHeight() * scale));
            customLayouts.add(imageSegmentBlurLayout);
            videoInfo.getCropConf().setCachedBlurBytes(null);
            MediaProxyTask mediaProxyTask = MediaProxyManager.getExecutedProxyTaskMap().get(videoInfo.getVideoId() + "_low");
            if (mediaProxyTask != null) {
                VideoInfo lowVideoInfo = mediaProxyTask.getVideoInfo();
                if (lowVideoInfo != null) {
                    lowVideoInfo.getCropConf().setLayouts(customLayouts);
                    lowVideoInfo.getCropConf().setCachedBlurBytes(null);
                }
            }
        } catch (Throwable e) {
            log.error("处理图像分割失败", e);
        }
    }
}
