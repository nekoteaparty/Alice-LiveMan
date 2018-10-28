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
