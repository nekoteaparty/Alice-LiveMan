/*
 * <Alice LiveMan>
 * Copyright (C) <2018>  <NekoSunflower>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package site.alice.liveman.utils;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.model.VideoInfo;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Component
public class FfmpegUtil {

    @Autowired
    private        LiveManSetting liveManSetting;
    private static String         moviePlaceHolder = "placeholder.png";
    private static String         audioPlaceHolder = "placeholder.mp3";

    @PostConstruct
    public void init() throws IOException {
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
    }

    public String getFfmpegPath() {
        return liveManSetting.getFfmpegPath();
    }

    public String buildFfmpegCmdLine(VideoInfo videoInfo, String broadcastAddress) {
        String loopCmdLine = "\t-re\t-stream_loop\t-1";
        loopCmdLine += "\t-i\t\"" + videoInfo.getChannelInfo().getMediaUrl() + "\"";
        if (videoInfo.isBanned()) {
            loopCmdLine += "\t-ac\t1\t-vcodec\th264\t-acodec\taac\t-vf\t\"[in]scale=32:-1[out]\"\t-b:v\t256K\t-b:a\t64K\t-preset\tultrafast\t-flush_packets\t1\t-f\tflv\t\"" + broadcastAddress + "\"";
        } else {
            loopCmdLine += "\t-vcodec\tcopy\t-acodec\taac\t-b:a\t128K\t-f\tflv\t\"" + broadcastAddress + "\"";
        }
        return loopCmdLine;
    }
}
