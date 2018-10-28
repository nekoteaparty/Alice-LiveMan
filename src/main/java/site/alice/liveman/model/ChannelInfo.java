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
