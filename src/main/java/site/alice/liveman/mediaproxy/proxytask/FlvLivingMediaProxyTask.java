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
package site.alice.liveman.mediaproxy.proxytask;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.utils.ProcessUtil;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.Proxy;
import java.net.URI;
import java.net.URLDecoder;

import static site.alice.liveman.mediaproxy.MediaProxyManager.getTempPath;


public class FlvLivingMediaProxyTask extends MediaProxyTask {

    private Long pid;

    @Autowired
    private LiveManSetting liveManSetting;

    public FlvLivingMediaProxyTask(String videoId, URI sourceUrl) {
        super(videoId, sourceUrl);
    }

    @Override
    protected void runTask() {
        new File(getTempPath() + "/flvLiving/").mkdirs();
        String livingFile = getTempPath() + "/flvLiving/" + getVideoId() + ".flv";
        new File(livingFile).delete();
        URI sourceUrl = getSourceUrl();
        String source;
        if (sourceUrl.getScheme().equals("file")) {
            try {
                source = URLDecoder.decode(sourceUrl.getAuthority(), "utf-8");
                pid = ProcessUtil.createProcess(liveManSetting.getFfmpegPath(), "\t-re\t-stream_loop\t-1\t-i\t\"" + source + "\"\t-flush_packets\t1\t-f\tflv\t" + livingFile + "\t-y", false);
            } catch (UnsupportedEncodingException ignored) {
            }
        } else {
            source = sourceUrl.toString();
            pid = ProcessUtil.createProcess(liveManSetting.getFfmpegPath(), "\t-re\t-i\t\"" + source + "\"\t-vf\tscale=320:180\t-vcodec\th264\t-acodec\taac\t-b:v\t128K\t-b:a\t16k\t-r\t15\t-preset\tultrafast\t-flush_packets\t1\t-f\tflv\t" + livingFile + "\t-y", false);
        }
        while (!getTerminated() && !ProcessUtil.waitProcess(pid, 1000)) ;
    }

    @Override
    protected void terminateTask() {
        if (pid != null && ProcessUtil.isProcessExist(pid)) {
            ProcessUtil.killProcess(pid);
        }
    }
}
