package site.alice.liveman.model;

import site.alice.liveman.mediaproxy.proxytask.MediaProxyTask;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChannelInfo implements Serializable {
    private String               channelName;
    private String               channelUrl;
    private String               mediaUrl;
    private List<MediaProxyTask> mediaProxyTasks;
    private Integer              sort;

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelUrl() {
        return channelUrl;
    }

    public void setChannelUrl(String channelUrl) {
        this.channelUrl = channelUrl;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public List<MediaProxyTask> getMediaProxyTasks() {
        return mediaProxyTasks;
    }

    public void setMediaProxyTasks(List<MediaProxyTask> mediaProxyTasks) {
        this.mediaProxyTasks = mediaProxyTasks;
    }

    public void addProxyTask(MediaProxyTask mediaProxyTask) {
        if (mediaProxyTasks == null) {
            mediaProxyTasks = new CopyOnWriteArrayList<>();
        }
        mediaProxyTask.setParentProxyTasks(mediaProxyTasks);
        mediaProxyTasks.add(mediaProxyTask);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChannelInfo that = (ChannelInfo) o;
        return Objects.equals(channelUrl, that.channelUrl) &&
                Objects.equals(mediaUrl, that.mediaUrl) &&
                Objects.equals(sort, that.sort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelUrl, mediaUrl, sort);
    }
}
