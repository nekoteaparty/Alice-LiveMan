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
package site.alice.liveman.model;

import site.alice.liveman.customlayout.CustomLayout;
import site.alice.liveman.service.broadcast.BroadcastServiceManager.BroadcastTask;

import java.io.Serializable;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;

public class VideoInfo implements Serializable {

    private ChannelInfo                    channelInfo;
    private String                         videoId;
    private String                         part;
    private String                         title;
    private String                         description;
    private URI                            videoInfoUrl;
    private URI                            mediaProxyUrl;
    private URI                            mediaUrl;
    private String                         mediaFormat;
    private int[]                          area;
    private boolean                        isVideoBanned;
    private boolean                        isAudioBanned;
    private boolean                        needRecord;
    private String                         encodeMethod;
    private byte[]                         encodeKey;
    private byte[]                         encodeIV;
    private AtomicReference<BroadcastTask> broadcastTask;
    private VideoCropConf                  cropConf;
    private Double                         frameRate;
    private String                         resolution;

    public VideoInfo(ChannelInfo channelInfo, String videoId, String title, URI videoInfoUrl, URI mediaUrl, String mediaFormat) {
        this.channelInfo = channelInfo;
        this.videoId = videoId;
        this.title = title;
        this.videoInfoUrl = videoInfoUrl;
        this.mediaUrl = mediaUrl;
        this.mediaFormat = mediaFormat;
        this.broadcastTask = new AtomicReference<>();
        this.cropConf = new VideoCropConf();
    }

    public URI getVideoInfoUrl() {
        return videoInfoUrl;
    }

    public void setVideoInfoUrl(URI videoInfoUrl) {
        this.videoInfoUrl = videoInfoUrl;
    }

    public String getVideoUnionId() {
        return videoId + (part == null ? "" : "." + part);
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

    public URI getMediaProxyUrl() {
        return mediaProxyUrl;
    }

    public void setMediaProxyUrl(URI mediaProxyUrl) {
        this.mediaProxyUrl = mediaProxyUrl;
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

    public boolean isVideoBanned() {
        return isVideoBanned;
    }

    public void setVideoBanned(boolean videoBanned) {
        isVideoBanned = videoBanned;
    }

    public boolean isAudioBanned() {
        return isAudioBanned;
    }

    public void setAudioBanned(boolean audioBanned) {
        isAudioBanned = audioBanned;
    }

    public boolean isNeedRecord() {
        return needRecord;
    }

    public void setNeedRecord(boolean needRecord) {
        this.needRecord = needRecord;
    }

    public int[] getArea() {
        return area;
    }

    public void setArea(int[] area) {
        this.area = area;
    }

    public BroadcastTask getBroadcastTask() {
        return broadcastTask.get();
    }

    public boolean setBroadcastTask(BroadcastTask broadcastTask) {
        return this.broadcastTask.compareAndSet(null, broadcastTask);
    }

    public boolean removeBroadcastTask(BroadcastTask broadcastTask) {
        return this.broadcastTask.compareAndSet(broadcastTask, null);
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

    public String getPart() {
        return part;
    }

    public void setPart(String part) {
        this.part = part;
    }

    public VideoCropConf getCropConf() {
        return cropConf;
    }

    public void setCropConf(VideoCropConf cropConf) {
        this.cropConf = cropConf;
    }

    public Double getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(Double frameRate) {
        this.frameRate = frameRate;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    @Override
    public String toString() {
        return "VideoInfo{" +
                "videoId='" + videoId + '\'' +
                ", part='" + part + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", mediaUrl=" + mediaUrl +
                ", mediaFormat='" + mediaFormat + '\'' +
                ", area=" + Arrays.toString(area) +
                ", isVideoBanned=" + isVideoBanned +
                ", isAudioBanned=" + isAudioBanned +
                ", encodeMethod='" + encodeMethod + '\'' +
                ", encodeKey=" + Arrays.toString(encodeKey) +
                ", encodeIV=" + Arrays.toString(encodeIV) +
                '}';
    }
}
