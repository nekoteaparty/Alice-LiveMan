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
package site.alice.liveman.model;

import java.io.Serializable;
import java.net.Proxy;
import java.net.URI;
import java.util.Arrays;

public class VideoInfo implements Serializable {

    private           ChannelInfo channelInfo;
    private           String      videoId;
    private           String      title;
    private           String      description;
    private           URI         mediaUrl;
    private           String      mediaFormat;
    private           String      encodeMethod;
    private           byte[]      encodeKey;
    private           byte[]      encodeIV;
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

    public String getEncodeMethod() {
        return encodeMethod;
    }

    public void setEncodeMethod(String encodeMethod) {
        this.encodeMethod = encodeMethod;
    }

    public byte[] getEncodeKey() {
        return encodeKey;
    }

    public void setEncodeKey(byte[] encodeKey) {
        this.encodeKey = encodeKey;
    }

    public byte[] getEncodeIV() {
        return encodeIV;
    }

    public void setEncodeIV(byte[] encodeIV) {
        this.encodeIV = encodeIV;
    }

    @Override
    public String toString() {
        return "VideoInfo{" +
                "videoId='" + videoId + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", mediaUrl=" + mediaUrl +
                ", mediaFormat='" + mediaFormat + '\'' +
                ", encodeMethod='" + encodeMethod + '\'' +
                ", encodeKey=" + Arrays.toString(encodeKey) +
                ", encodeIV=" + Arrays.toString(encodeIV) +
                '}';
    }
}
