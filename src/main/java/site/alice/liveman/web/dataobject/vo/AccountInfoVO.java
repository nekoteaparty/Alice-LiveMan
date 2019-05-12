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

package site.alice.liveman.web.dataobject.vo;

import java.util.HashMap;

public class AccountInfoVO {
    private String                 accountId;
    private String                 accountSite;
    private String                 nickname;
    private String                 description;
    private String                 roomId;
    private boolean                joinAutoBalance;
    private boolean                admin;
    private boolean                saved;
    private boolean                disable;
    private boolean                postBiliDynamic;
    private boolean                autoRoomTitle;
    private long                   point;
    private HashMap<Integer, Long> billTimeMap;
    private long                   timestamp = System.currentTimeMillis();

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

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public boolean isJoinAutoBalance() {
        return joinAutoBalance;
    }

    public void setJoinAutoBalance(boolean joinAutoBalance) {
        this.joinAutoBalance = joinAutoBalance;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    public boolean isDisable() {
        return disable;
    }

    public void setDisable(boolean disable) {
        this.disable = disable;
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

    public long getPoint() {
        return point;
    }

    public void setPoint(long point) {
        this.point = point;
    }

    public HashMap<Integer, Long> getBillTimeMap() {
        return billTimeMap;
    }

    public void setBillTimeMap(HashMap<Integer, Long> billTimeMap) {
        this.billTimeMap = billTimeMap;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
