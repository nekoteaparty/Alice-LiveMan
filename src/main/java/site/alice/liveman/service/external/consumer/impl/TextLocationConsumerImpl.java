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
import site.alice.liveman.customlayout.impl.RectangleBlurLayout;
import site.alice.liveman.mediaproxy.MediaProxyManager;
import site.alice.liveman.mediaproxy.proxytask.MediaProxyTask;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.service.broadcast.BroadcastServiceManager;
import site.alice.liveman.service.external.TextLocation;
import site.alice.liveman.service.external.consumer.TextLocationConsumer;
import site.alice.liveman.utils.ProcessUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class TextLocationConsumerImpl implements TextLocationConsumer {

    private VideoInfo videoInfo;

    public TextLocationConsumerImpl(VideoInfo videoInfo) {
        this.videoInfo = videoInfo;
    }

    @Override
    public void accept(List<TextLocation> textLocations, BufferedImage bufferedImage) {
        log.info("评论区识别[" + videoInfo.getVideoId() + "]:" + textLocations);
        try {
            File easyDlDir = new File("./easydl/");
            easyDlDir.mkdirs();
            String dashFileName;
            if (textLocations.size() > 1) {
                // 可能是误识别，保存这次的识别记录
                dashFileName = videoInfo.getVideoId() + "_" + System.currentTimeMillis();
            } else {
                dashFileName = videoInfo.getVideoId() + "_" + System.currentTimeMillis() / 600000;
            }
            ImageIO.write(bufferedImage, "jpg", new File(easyDlDir + "/" + dashFileName + "_raw.jpg"));
            try (OutputStream os = new FileOutputStream(easyDlDir + "/" + dashFileName + "_rect.txt")) {
                for (TextLocation textLocation : textLocations) {
                    os.write((textLocation.toString() + "\n").getBytes());
                }
            }
            textLocations.removeIf(textLocation -> textLocation.getScore() < 0.5);
            if (videoInfo.getTextLocations() == null) {
                videoInfo.setTextLocations(new ArrayList<>(textLocations));
            }
            // 清理已有区域
            for (Iterator<TextLocation> iterator = videoInfo.getTextLocations().iterator(); iterator.hasNext(); ) {
                TextLocation textLocation = iterator.next();
                if (textLocation.getLastHitTime() == null) {
                    textLocation.setLastHitTime(System.currentTimeMillis());
                }
                boolean hasContains = false;
                for (Iterator<TextLocation> newIterator = textLocations.iterator(); newIterator.hasNext(); ) {
                    TextLocation location = newIterator.next();
                    Rectangle oldRectangle = new Rectangle(textLocation.getRectangle());
                    oldRectangle.grow(20, 20); // 容差±20px
                    Rectangle newRectangle = new Rectangle(location.getRectangle());
                    newRectangle.grow(20, 20); // 容差±20px
                    if (oldRectangle.contains(location.getRectangle())) {
                        hasContains = true;
                        newIterator.remove(); // 找到匹配的已有区域，从新增区域中删除
                        textLocation.getRectangle().add(location.getRectangle());
                        if (newRectangle.contains(textLocation.getRectangle())) {
                            textLocation.setLastHitTime(System.currentTimeMillis());
                        } else if (System.currentTimeMillis() - textLocation.getLastHitTime() > 30000) {
                            // 评论区没有改变位置，但是被缩小
                            textLocation.getRectangle().setBounds(location.getRectangle());
                        }
                    }
                }
                if (!hasContains && System.currentTimeMillis() - textLocation.getLastHitTime() > 30000) {
                    // 如果某个区域超过30秒没有被命中，则淘汰
                    iterator.remove();
                }
            }

            // 新增加的区域
            videoInfo.getTextLocations().addAll(textLocations);

            // 设置新的自定义渲染层
            double scale = 720.0 / bufferedImage.getHeight();
            CopyOnWriteArrayList<CustomLayout> customLayouts = videoInfo.getCropConf().getLayouts();
            customLayouts.removeIf(customLayout -> customLayout instanceof RectangleBlurLayout);
            for (TextLocation textLocation : videoInfo.getTextLocations()) {
                RectangleBlurLayout rectangleBlurLayout = new RectangleBlurLayout();
                rectangleBlurLayout.setVideoInfo(videoInfo);
                rectangleBlurLayout.setX((int) (textLocation.getRectangle().getX() * scale));
                rectangleBlurLayout.setY((int) (textLocation.getRectangle().getY() * scale));
                rectangleBlurLayout.setWidth((int) (textLocation.getRectangle().getWidth() * scale));
                rectangleBlurLayout.setHeight((int) (textLocation.getRectangle().getHeight() * scale));
                customLayouts.add(rectangleBlurLayout);
            }
            videoInfo.getCropConf().setCachedBlurBytes(null);
            MediaProxyTask mediaProxyTask = MediaProxyManager.getExecutedProxyTaskMap().get(videoInfo.getVideoId() + "_low");
            if (mediaProxyTask != null) {
                VideoInfo lowVideoInfo = mediaProxyTask.getVideoInfo();
                if (lowVideoInfo != null) {
                    lowVideoInfo.getCropConf().setLayouts(customLayouts);
                    lowVideoInfo.getCropConf().setCachedBlurBytes(null);
                    if (lowVideoInfo.getCropConf().getBlurSize() != 5) {
                        videoInfo.getCropConf().setBlurSize(5);
                        lowVideoInfo.getCropConf().setBlurSize(5);
                        BroadcastServiceManager.BroadcastTask broadcastTask = videoInfo.getBroadcastTask();
                        if (broadcastTask != null) {
                            ProcessUtil.killProcess(broadcastTask.getPid());
                        }
                    }
                }
            }
        } catch (Throwable e) {
            log.error("处理评论区识别失败", e);
        }
    }
}