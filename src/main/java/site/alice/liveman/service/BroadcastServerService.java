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

package site.alice.liveman.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.alice.liveman.config.SettingConfig;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.model.ServerInfo;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.utils.ProcessUtil;
import site.alice.liveman.web.dataobject.ActionResult;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BroadcastServerService {

    @Autowired
    private LiveManSetting liveManSetting;
    @Autowired
    private SettingConfig  settingConfig;

    public ServerInfo getAvailableServer(VideoInfo videoInfo) {
        CopyOnWriteArraySet<ServerInfo> servers = liveManSetting.getServers();
        List<ServerInfo> availableServers = servers.stream().filter(server -> server.getCurrentVideo() == null).collect(Collectors.toList());
        while (!availableServers.isEmpty()) {
            ServerInfo serverInfo = availableServers.get((int) (Math.random() * availableServers.size()));
            if (serverInfo.setCurrentVideo(videoInfo)) {
                log.info("转播服务器调度成功[" + serverInfo.getRemark() + "@" + serverInfo.getAddress() + ":" + serverInfo.getPort() + "] => videoId=" + videoInfo.getVideoId());
                return serverInfo;
            } else {
                availableServers.remove(serverInfo);
            }
        }
        throw new RuntimeException("没有找到空闲的转播服务器!");
    }

    public void addServer(ServerInfo serverInfo) throws Exception {
        String scpCmd = String.format("sshpass\t-p\t%s\tscp\t-o\tStrictHostKeyChecking=no\t-P\t%s\t-r\t%s\t%s@%s:%s", serverInfo.getPassword(), serverInfo.getPort(), liveManSetting.getFfmpegPath(), serverInfo.getUsername(), serverInfo.getAddress(), liveManSetting.getFfmpegPath());
        long process = ProcessUtil.createProcess(scpCmd, "install-scp" + serverInfo.getAddress(), false);
        ProcessUtil.waitProcess(process);
        String sshCmd = String.format("sshpass\t-p\t%s\tssh\t-o\tStrictHostKeyChecking=no\t-p\t%s\t%s@%s\tchmod 777 %s", serverInfo.getPassword(), serverInfo.getPort(), serverInfo.getUsername(), serverInfo.getAddress(), liveManSetting.getFfmpegPath());
        process = ProcessUtil.createProcess(sshCmd, "install-chmod" + serverInfo.getAddress(), false);
        ProcessUtil.waitProcess(process);
        liveManSetting.getServers().add(serverInfo);
        settingConfig.saveSetting(liveManSetting);
    }

    public void releaseServer(VideoInfo videoInfo) {
        CopyOnWriteArraySet<ServerInfo> servers = liveManSetting.getServers();
        for (ServerInfo server : servers) {
            server.removeCurrentVideo(videoInfo);
        }
    }

}
