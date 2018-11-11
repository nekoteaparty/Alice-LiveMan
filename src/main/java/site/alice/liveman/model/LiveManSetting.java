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

import java.util.concurrent.CopyOnWriteArrayList;

public class LiveManSetting {
    private String[]                          bannedYoutubeChannel;
    private String[]                          bannedKeywords;
    private String                            tempPath;
    private String                            ffmpegPath;
    private boolean                           postBiliDynamic;
    private CopyOnWriteArrayList<AccountInfo> accounts;
    private CopyOnWriteArrayList<ChannelInfo> channels;

    public String[] getBannedYoutubeChannel() {
        return bannedYoutubeChannel;
    }

    public void setBannedYoutubeChannel(String[] bannedYoutubeChannel) {
        this.bannedYoutubeChannel = bannedYoutubeChannel;
    }

    public String[] getBannedKeywords() {
        return bannedKeywords;
    }

    public void setBannedKeywords(String[] bannedKeywords) {
        this.bannedKeywords = bannedKeywords;
    }

    public String getTempPath() {
        return tempPath;
    }

    public void setTempPath(String tempPath) {
        this.tempPath = tempPath;
    }

    public String getFfmpegPath() {
        return ffmpegPath;
    }

    public void setFfmpegPath(String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
    }

    public boolean isPostBiliDynamic() {
        return postBiliDynamic;
    }

    public void setPostBiliDynamic(boolean postBiliDynamic) {
        this.postBiliDynamic = postBiliDynamic;
    }

    public CopyOnWriteArrayList<AccountInfo> getAccounts() {
        return accounts;
    }

    public void setAccounts(CopyOnWriteArrayList<AccountInfo> accounts) {
        this.accounts = accounts;
    }

    public CopyOnWriteArrayList<ChannelInfo> getChannels() {
        return channels;
    }

    public void setChannels(CopyOnWriteArrayList<ChannelInfo> channels) {
        this.channels = channels;
    }

    public AccountInfo findByAccountId(String accountId) {
        for (AccountInfo account : accounts) {
            if (account.getAccountId().equals(accountId)) {
                return account;
            }
        }
        return null;
    }
}
