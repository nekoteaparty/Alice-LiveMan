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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import site.alice.liveman.jenum.VideoBannedTypeEnum;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.model.ServerInfo;
import site.alice.liveman.model.VideoCropConf;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.service.BroadcastServerService;

@Component
@Slf4j
public class FfmpegUtil {

    private static BroadcastServerService broadcastServerService;
    private static LiveManSetting         liveManSetting;

    @Autowired
    public void setBroadcastServerService(BroadcastServerService broadcastServerService) {
        FfmpegUtil.broadcastServerService = broadcastServerService;
    }

    @Autowired
    public void setLiveManSetting(LiveManSetting liveManSetting) {
        FfmpegUtil.liveManSetting = liveManSetting;
    }

    public static String buildKeyFrameCmdLine(String mediaUrl, String fileName) {
        return liveManSetting.getFfmpegPath() + "\t-i\t" + mediaUrl + "\t-vframes\t1\t-y\t" + fileName;
    }

    public static String buildFfmpegCmdLine(VideoInfo videoInfo, String broadcastAddress) {
        String ffmpegCmdLine = buildLocalFfmpegCmdLine(videoInfo, broadcastAddress);
        if (videoInfo.getCropConf().getVideoBannedType() == VideoBannedTypeEnum.AREA_SCREEN) {
            ServerInfo availableServer = broadcastServerService.getAvailableServer(videoInfo);
            return String.format("sshpass\t-p\t%s\tssh\t-o\tStrictHostKeyChecking=no\t-tt\t-p\t%s\t%s@%s\t", availableServer.getPassword(),
                    availableServer.getPort(), availableServer.getUsername(), availableServer.getAddress()) +
                    ffmpegCmdLine.replaceAll("\t", " ");
        } else {
            return ffmpegCmdLine;
        }
    }

    public static String buildLocalFfmpegCmdLine(VideoInfo videoInfo, String broadcastAddress) {
        String cmdLine = liveManSetting.getFfmpegPath() + "\t-re\t-i\t\"" + videoInfo.getMediaProxyUrl() + "\"";
        if (videoInfo.isAudioBanned()) {
            cmdLine += "\t-ac\t1";
        }
        VideoCropConf cropConf = videoInfo.getCropConf();
        if (cropConf.getVideoBannedType() == VideoBannedTypeEnum.FULL_SCREEN) {
            cmdLine += "\t-vf\t\"[in]scale=32:-1[out]\"";
            cmdLine += "\t-vcodec\th264";
        } else if (cropConf.getVideoBannedType() == VideoBannedTypeEnum.AREA_SCREEN) {
            String areaCmd = "\t-vf\t\"[ina]fps=30,scale=854:480[outa];[outa]split[blurin][originalin];[blurin]crop=%s:%s:%s:%s,boxblur=3:1[blurout];[originalin][blurout]overlay=x=%s:y=%s[out]\"";
            cmdLine += String.format(areaCmd, cropConf.getCtrlWidth(), cropConf.getCtrlHeight(), cropConf.getCtrlLeft(), cropConf.getCtrlTop(), cropConf.getCtrlLeft(), cropConf.getCtrlTop());
            cmdLine += "\t-vcodec\th264\t-preset\tultrafast";
        } else {
            cmdLine += "\t-vcodec\tcopy";
        }
        cmdLine += "\t-acodec\taac\t-b:a\t132K\t-f\tflv\t\"" + broadcastAddress + "\"";
        return cmdLine;
    }
}
