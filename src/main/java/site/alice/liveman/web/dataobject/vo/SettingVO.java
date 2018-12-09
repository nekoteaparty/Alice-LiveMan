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

public class SettingVO {
    private String   ffmpegPath;
    private boolean  postBiliDynamic;
    private String[] bannedYoutubeChannel;
    private String[] bannedKeywords;
    private String   defaultResolution;
    private String   baseUrl;
    private boolean  hasOneDriveToken;

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

    public String[] getBannedYoutubeChannelArray() {
        return bannedYoutubeChannel;
    }

    public String[] getBannedKeywordsArray() {
        return bannedKeywords;
    }

    public String getBannedYoutubeChannel() {
        return String.join(",", bannedYoutubeChannel);
    }

    public void setBannedYoutubeChannel(String bannedYoutubeChannel) {
        this.bannedYoutubeChannel = bannedYoutubeChannel.split(",");
    }

    public String getBannedKeywords() {
        return String.join(",", bannedKeywords);
    }

    public void setBannedKeywords(String bannedKeywords) {
        this.bannedKeywords = bannedKeywords.split(",");
    }

    public String getDefaultResolution() {
        return defaultResolution;
    }

    public void setDefaultResolution(String defaultResolution) {
        this.defaultResolution = defaultResolution;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isHasOneDriveToken() {
        return hasOneDriveToken;
    }

    public void setHasOneDriveToken(boolean hasOneDriveToken) {
        this.hasOneDriveToken = hasOneDriveToken;
    }
}
