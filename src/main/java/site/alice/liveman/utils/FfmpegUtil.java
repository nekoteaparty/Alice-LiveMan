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
        return liveManSetting.getFfmpegPath() + "\t-i\t" + srcFile + "\t-r\t30\t-copyts\t-acodec\tcopy\t-qscale:v\t12\t" + dictFile + "\t-y";
    }

    public static String buildFfmpegCmdLine(VideoInfo videoInfo, String broadcastAddress) {
        String cmdLine = "\t-re\t-i\t\"" + videoInfo.getMediaProxyUrl() + "\"";
        if (videoInfo.isAudioBanned()) {
            cmdLine += "\t-ac\t1";
        }
        VideoCropConf cropConf = videoInfo.getCropConf();
        if (cropConf.getVideoBannedType() == VideoBannedTypeEnum.FULL_SCREEN) {
            cmdLine += "\t-vf\t\"[in]scale=32:-1[out]\"";
            cmdLine += "\t-vcodec\th264";
        } else if (cropConf.getVideoBannedType() == VideoBannedTypeEnum.AREA_SCREEN) {
            String areaCmd = "\t-vf\t\"[ina]fps=30,scale=1280:720[outa];[outa]split[blurin][originalin];[blurin]crop=%s:%s:%s:%s,boxblur=" + cropConf.getBlurSize() + ":" + cropConf.getBlurSize() + "[blurout];[originalin][blurout]overlay=x=%s:y=%s[out]\"";
            cmdLine += String.format(areaCmd, cropConf.getCtrlWidth(), cropConf.getCtrlHeight(), cropConf.getCtrlLeft(), cropConf.getCtrlTop(), cropConf.getCtrlLeft(), cropConf.getCtrlTop());
            cmdLine += "\t-vcodec\th264\t-preset\tultrafast";
        } else if (cropConf.getVideoBannedType() == VideoBannedTypeEnum.CUSTOM_SCREEN) {
            cmdLine += "\t-framerate\t2\t-loop\t1\t-i\t\"" + String.format(CUSTOM_SCREEN_URL, videoInfo.getVideoId()) + "\"\t-filter_complex\t\"[0:v]fps=30,scale=1280x720[outv0];[outv0][1:v]overlay=0:0\"\t-vcodec\th264\t-preset\tultrafast";
        } else {
            cmdLine += "\t-vcodec\tcopy";
        }
        cmdLine += "\t-acodec\taac\t-b:a\t130K\t-f\tflv\t\"" + broadcastAddress + "\"";
        return liveManSetting.getFfmpegPath() + cmdLine;
    }
}
