package site.alicemononobe.liveman.model;

import java.io.Serializable;
import java.net.Proxy;
import java.net.URI;

public class VideoInfo implements Serializable {

    private           ChannelInfo channelInfo;
    private           String      videoId;
    private           String      title;
    private           String      description;
    private           URI         mediaUrl;
    private           String      mediaFormat;
    private transient Proxy       networkProxy;

    public VideoInfo(ChannelInfo channelInfo, String videoId, String title, URI mediaUrl, String mediaFormat) {
        this.channelInfo = channelInfo;
        this.videoId = videoId;
        this.title = title;
        this.mediaUrl = mediaUrl;
        this.mediaFormat = mediaFormat;
    }

    public ChannelInfo getChannelInfo() {
        return channelInfo;
    }

    public void setChannelInfo(ChannelInfo channelInfo) {
        this.channelInfo = channelInfo;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public URI getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(URI mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getMediaFormat() {
        return mediaFormat;
    }

    public void setMediaFormat(String mediaFormat) {
        this.mediaFormat = mediaFormat;
    }

    public Proxy getNetworkProxy() {
        return networkProxy;
    }

    public void setNetworkProxy(Proxy networkProxy) {
        this.networkProxy = networkProxy;
    }
}
