package site.alicemononobe.liveman.service.live;

import site.alicemononobe.liveman.mediaproxy.MediaProxy;
import site.alicemononobe.liveman.mediaproxy.MediaProxyManager;
import site.alicemononobe.liveman.mediaproxy.proxytask.MediaProxyTask;
import site.alicemononobe.liveman.model.ChannelInfo;
import site.alicemononobe.liveman.model.VideoInfo;

import java.net.URI;

public abstract class LiveService {

    public URI getLiveVideoAddress(ChannelInfo channelInfo) throws Exception {
        VideoInfo videoInfo = getLiveVideoInfo(channelInfo);
        if (videoInfo != null) {
            MediaProxyTask mediaProxyTask = MediaProxyManager.createProxy(videoInfo);
            channelInfo.addProxyTask(mediaProxyTask);
            return mediaProxyTask.getTargetUrl();
        } else {
            return null;
        }
    }

    protected abstract VideoInfo getLiveVideoInfo(ChannelInfo channelInfo) throws Exception;

    protected abstract boolean isMatch(URI channelUrl);
}
