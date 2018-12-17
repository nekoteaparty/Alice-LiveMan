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

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.ConcurrentSkipListSet;

public class LiveManSetting {
    private String[]                           bannedYoutubeChannel;
    private String[]                           bannedKeywords;
    private String                             tempPath;
    private String                             ffmpegPath;
    private String                             defaultResolution;
    private String                             baseUrl;
    private String                             oneDriveClientId;
    private String                             oneDriveClientSecret;
    private String                             oneDriveToken;
    private ConcurrentSkipListSet<AccountInfo> accounts;
    private ConcurrentSkipListSet<ChannelInfo> channels;
    private Proxy                              proxy;

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

    public String getDefaultResolution() {
        return defaultResolution;
    }

    public void setDefaultResolution(String defaultResolution) {
        this.defaultResolution = defaultResolution;
    }

    public ConcurrentSkipListSet<AccountInfo> getAccounts() {
        return accounts;
    }

    public void setAccounts(ConcurrentSkipListSet<AccountInfo> accounts) {
        this.accounts = accounts;
    }

    public ConcurrentSkipListSet<ChannelInfo> getChannels() {
        return channels;
    }

    public void setChannels(ConcurrentSkipListSet<ChannelInfo> channels) {
        this.channels = channels;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getOneDriveClientId() {
        return oneDriveClientId;
    }

    public void setOneDriveClientId(String oneDriveClientId) {
        this.oneDriveClientId = oneDriveClientId;
    }

    public String getOneDriveClientSecret() {
        return oneDriveClientSecret;
    }

    public void setOneDriveClientSecret(String oneDriveClientSecret) {
        this.oneDriveClientSecret = oneDriveClientSecret;
    }

    public String getOneDriveToken() {
        return oneDriveToken;
    }

    public void setOneDriveToken(String oneDriveToken) {
        this.oneDriveToken = oneDriveToken;
    }

    @JSONField(serialize = false)
    public Proxy getProxy() {
        return proxy;
    }

    public ProxyInfo getProxyInfo() {
        if (proxy != null) {
            ProxyInfo proxyInfo = new ProxyInfo();
            InetSocketAddress address = (InetSocketAddress) proxy.address();
            proxyInfo.setHost(address.getHostName());
            proxyInfo.setPort(address.getPort());
            proxyInfo.setType(proxy.type());
            return proxyInfo;
        }
        return null;
    }

    public void setProxyInfo(ProxyInfo proxyInfo) {
        this.proxy = new Proxy(proxyInfo.getType(), new InetSocketAddress(proxyInfo.getHost(), proxyInfo.getPort()));
    }

    public AccountInfo findByAccountId(String accountId) {
        if (accountId == null) {
            return null;
        }
        for (AccountInfo account : accounts) {
            if (account.getAccountId().equals(accountId)) {
                return account;
            }
        }
        return null;
    }
}
