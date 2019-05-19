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

import com.alibaba.fastjson.annotation.JSONField;
import site.alice.liveman.jenum.VideoResolutionEnum;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class AccountInfo implements Comparable<AccountInfo> {

    private String                           accountId;
    private String                           accountSite;
    private String                           cookies;
    private String                           nickname;
    private String                           description;
    private String                           roomId;
    private String                           uid;
    private boolean                          joinAutoBalance;
    private boolean                          admin;
    private boolean                          disable;
    private boolean                          postBiliDynamic;
    private boolean                          autoRoomTitle;
    private AtomicLong                       point;
    private ConcurrentHashMap<Integer, Long> billTimeMap;
    private CopyOnWriteArrayList<BillRecord> billRecords;
    private VideoResolutionEnum              broadcastResolution;
    @JSONField(serialize = false)
    private AtomicReference<VideoInfo>       currentVideo = new AtomicReference<>();

    public AccountInfo() {
        point = new AtomicLong();
        billTimeMap = new ConcurrentHashMap<>();
        billRecords = new CopyOnWriteArrayList<>();
    }

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
        return this.currentVideo.compareAndSet(currentVideo, null) || this.currentVideo.compareAndSet(null, null);
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean isDisable() {
        return disable;
    }

    public void setDisable(boolean disable) {
        this.disable = disable;
    }

    public Long getPoint() {
        return point.get();
    }

    public void setPoint(AtomicLong point) {
        this.point = point;
    }

    public long changePoint(long delta, String remark) {
        BillRecord billRecord = new BillRecord(delta, remark);
        billRecords.add(0, billRecord);
        if (billRecords.size() > 100) {
            billRecords = new CopyOnWriteArrayList<>(billRecords.subList(0, 100));
        }
        return this.point.addAndGet(delta);
    }

    public ConcurrentHashMap<Integer, Long> getBillTimeMap() {
        return billTimeMap;
    }

    public void setBillTimeMap(ConcurrentHashMap<Integer, Long> billTimeMap) {
        this.billTimeMap = billTimeMap;
    }

    public CopyOnWriteArrayList<BillRecord> getBillRecords() {
        return billRecords;
    }

    public void setBillRecords(CopyOnWriteArrayList<BillRecord> billRecords) {
        this.billRecords = billRecords;
    }

    public boolean isPostBiliDynamic() {
        return postBiliDynamic;
    }

    public void setPostBiliDynamic(boolean postBiliDynamic) {
        this.postBiliDynamic = postBiliDynamic;
    }

    public boolean isAutoRoomTitle() {
        return autoRoomTitle;
    }

    public void setAutoRoomTitle(boolean autoRoomTitle) {
        this.autoRoomTitle = autoRoomTitle;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public VideoResolutionEnum getBroadcastResolution() {
        return broadcastResolution;
    }

    public void setBroadcastResolution(VideoResolutionEnum broadcastResolution) {
        this.broadcastResolution = broadcastResolution;
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
    public int compareTo(AccountInfo o) {
        return this.getAccountId().compareTo(o.getAccountId());
    }
}
