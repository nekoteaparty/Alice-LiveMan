package site.alice.liveman.service.live;

import site.alice.liveman.mediaproxy.MediaProxyManager;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.mediaproxy.proxytask.MediaProxyTask;

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
