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

import com.sun.jna.Platform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import site.alice.liveman.jenum.VideoBannedTypeEnum;
import site.alice.liveman.mediaproxy.MediaProxyManager;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.model.ServerInfo;
import site.alice.liveman.model.VideoCropConf;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.service.BroadcastServerService;

import java.io.File;

@Component
@Slf4j
public class FfmpegUtil {

    private static       LiveManSetting liveManSetting;
    private static final String         CUSTOM_SCREEN_URL = "http://" + MediaProxyManager.getIpAddress() + ":8080/api/drawing/screen/%s";
    private static final String         BOXBLUR_MASK_URL  = "http://" + MediaProxyManager.getIpAddress() + ":8080/api/drawing/mask/%s";

    @Autowired
    public void setLiveManSetting(LiveManSetting liveManSetting) {
        FfmpegUtil.liveManSetting = liveManSetting;
    }

    public static String buildKeyFrameCmdLine(String mediaUrl, String fileName) {
        return liveManSetting.getFfmpegPath() + "\t-i\t" + mediaUrl + "\t-vframes\t1\t-y\t" + fileName;
    }

    public static String buildToLowFrameRateCmdLine(File srcFile, File dictFile) {
        return liveManSetting.getFfmpegPath() + "\t-i\t" + srcFile + "\t-r\t30\t-s\t1280x720\t-copyts\t-acodec\tcopy\t-qscale:v\t12\t" + dictFile + "\t-y";
    }

    public static String buildFfmpegCmdLine(VideoInfo videoInfo, String broadcastAddress) {
        String cmdLine = "\t-re\t-i\t\"" + videoInfo.getMediaProxyUrl() + "\"";
        VideoCropConf cropConf = videoInfo.getCropConf();
        if (cropConf.getVideoBannedType() == VideoBannedTypeEnum.FULL_SCREEN) {
            cmdLine += "\t-vf\t\"[in]scale=32:-1[out]\"";
            cmdLine += "\t-vcodec\th264";
        } else if (cropConf.getVideoBannedType() == VideoBannedTypeEnum.CUSTOM_SCREEN) {
            String filter;
            if (cropConf.getBlurSize() > 0) {
                cmdLine += "\t-framerate\t1\t-loop\t1\t-i\t\"" + String.format(BOXBLUR_MASK_URL, videoInfo.getVideoId()) + "\"";
                if (liveManSetting.getPreReEncode()) {
                    filter = "[0:v]smartblur=" + cropConf.getBlurSize() + ":1[blur];[1:v]fps=30[mask];[blur][mask]alphamerge[alf];[0:v][alf]overlay[v];[v][2:v]overlay";
                } else {
                    filter = "[0:v]fps=30,scale=1280x720,split=2[ref0][ref1];[ref0]smartblur=" + cropConf.getBlurSize() + ":1[blur];[1:v]fps=30[mask];[blur][mask]alphamerge[alf];[ref1][alf]overlay[v];[v][2:v]overlay";
                }
            } else {
                filter = "[0:v]fps=30,scale=1280x720[v];[v][1:v]overlay";
            }
            cmdLine += "\t-framerate\t1\t-loop\t1\t-i\t\"" + String.format(CUSTOM_SCREEN_URL, videoInfo.getVideoId()) + "\"\t-filter_complex\t\"" + filter + "\"\t-vcodec\th264\t-preset\tultrafast";
        } else {
            cmdLine += "\t-vcodec\tcopy";
        }
        if (videoInfo.isAudioBanned()) {
            cmdLine += "\t-ac\t1";
        }
        cmdLine += "\t-acodec\taac\t-b:a\t130K\t-f\tflv\t\"" + broadcastAddress + "\"";
        return liveManSetting.getFfmpegPath() + cmdLine;
    }
}
