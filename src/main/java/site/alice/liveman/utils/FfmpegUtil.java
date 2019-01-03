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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import site.alice.liveman.model.VideoInfo;

import java.io.File;
import java.io.FileOutputStream;

public class FfmpegUtil {

    private static String moviePlaceHolder = "placeholder.png";
    private static String audioPlaceHolder = "placeholder.mp3";

    static {
        try {
            File moviePlaceFile = new File("./" + moviePlaceHolder);
            if (!moviePlaceFile.exists()) {
                ClassPathResource resource = new ClassPathResource(moviePlaceHolder);
                IOUtils.copy(resource.getInputStream(), new FileOutputStream(moviePlaceFile));
            }
            moviePlaceHolder = FilenameUtils.separatorsToUnix(moviePlaceFile.getAbsolutePath());
            File audioPlaceFile = new File("./" + audioPlaceHolder);
            if (!audioPlaceFile.exists()) {
                ClassPathResource resource = new ClassPathResource(audioPlaceHolder);
                IOUtils.copy(resource.getInputStream(), new FileOutputStream(audioPlaceFile));
            }
            audioPlaceHolder = FilenameUtils.separatorsToUnix(audioPlaceFile.getAbsolutePath());
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static String buildKeyFrameCmdLine(String mediaUrl, String fileName) {
        return " \t-i\t" + mediaUrl + "\t-vframes\t1\t-y\t" + fileName;
    }

    public static String buildFfmpegCmdLine(VideoInfo videoInfo, String broadcastAddress) {
        String loopCmdLine = "\t-i\t\"" + videoInfo.getChannelInfo().getMediaUrl() + "\"";
        if (videoInfo.isAudioBanned()) {
            loopCmdLine += "\t-ac\t1";
        }
        if (videoInfo.isVideoBanned()) {
            loopCmdLine += "\t-vf\t\"[in]scale=32:-1[out]\"";
            loopCmdLine += "\t-vcodec\th264";
        } else {
            loopCmdLine += "\t-vcodec\tcopy";
        }
        loopCmdLine += "\t-acodec\taac\t-b:a\t128K\t-f\tflv\t\"" + broadcastAddress + "\"";
        return loopCmdLine;
    }
}
