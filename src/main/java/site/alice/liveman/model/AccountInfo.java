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

import com.alibaba.fastjson.annotation.JSONField;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class AccountInfo implements Comparable<AccountInfo> {

    private String                     accountId;
    private String                     accountSite;
    private String                     cookies;
    private String                     nickname;
    private String                     description;
    private String                     roomId;
    private boolean                    joinAutoBalance;
    @JSONField(serialize = false)
    private AtomicReference<VideoInfo> currentVideo = new AtomicReference<>();

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountSite() {
        return accountSite;
    }

    public void setAccountSite(String accountSite) {
        this.accountSite = accountSite;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getCookies() {
        return cookies;
    }

    public void setCookies(String cookies) {
        this.cookies = cookies;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isJoinAutoBalance() {
        return joinAutoBalance;
    }

    public void setJoinAutoBalance(boolean joinAutoBalance) {
        this.joinAutoBalance = joinAutoBalance;
    }

    public VideoInfo getCurrentVideo() {
        return currentVideo.get();
    }

    public boolean setCurrentVideo(VideoInfo currentVideo) {
        return this.currentVideo.compareAndSet(null, currentVideo);
    }

    public boolean removeCurrentVideo(VideoInfo currentVideo) {
        return this.currentVideo.compareAndSet(currentVideo, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountInfo that = (AccountInfo) o;
        return Objects.equals(accountSite, that.accountSite) &&
                Objects.equals(roomId, that.roomId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountSite, roomId);
    }

    @Override
    public int compareTo(@NotNull AccountInfo o) {
        return this.getAccountId().compareTo(o.getAccountId());
    }
}
