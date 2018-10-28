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

import site.alice.liveman.utils.ProcessUtil;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.Proxy;
import java.net.URI;
import java.net.URLDecoder;

import static site.alice.liveman.mediaproxy.MediaProxyManager.getTempPath;


public class FlvLivingMediaProxyTask extends MediaProxyTask {

    private Long pid;

    public FlvLivingMediaProxyTask(String videoId, URI sourceUrl, Proxy proxy) {
        super(videoId, sourceUrl, proxy);
    }

    @Override
    protected void runTask() {
        new File(getTempPath() + "/flvLiving/").mkdirs();
        URI sourceUrl = getSourceUrl();
        String source;
        if (sourceUrl.getScheme().equals("file")) {
            try {
                source = URLDecoder.decode(sourceUrl.getAuthority(), "utf-8");
                pid = ProcessUtil.createProcess(System.getenv("SystemRoot") + "/system32/ffmpeg.exe", " -re -stream_loop -1 -i \"" + source + "\" -flush_packets 1 -f flv " + getTempPath() + "/flvLiving/" + getVideoId() + ".flv -y", false);
            } catch (UnsupportedEncodingException ignored) {
            }
        } else {
            source = sourceUrl.toString();
            pid = ProcessUtil.createProcess(System.getenv("SystemRoot") + "/system32/ffmpeg.exe", " -re -i \"" + source + "\" -vf scale=320:180 -vcodec h264 -acodec aac -b:v 128K -b:a 16k -r 15 -preset ultrafast -flush_packets 1 -f flv " + getTempPath() + "/flvLiving/" + getVideoId() + ".flv -y", false);
        }
        ProcessUtil.waitProcess(pid);
    }

    @Override
    protected void terminateTask() {
        if (pid != null && ProcessUtil.isProcessExist(pid)) {
            ProcessUtil.killProcess(pid);
        }
    }
}
