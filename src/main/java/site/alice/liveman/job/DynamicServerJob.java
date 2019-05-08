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

package site.alice.liveman.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import site.alice.liveman.config.SettingConfig;
import site.alice.liveman.jenum.VideoResolutionEnum;
import site.alice.liveman.model.AccountInfo;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.model.ServerInfo;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.service.broadcast.BroadcastServiceManager.BroadcastTask;
import site.alice.liveman.service.external.DynamicServerService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
public class DynamicServerJob {

    @Autowired
    private SettingConfig        settingConfig;
    @Autowired
    private LiveManSetting       liveManSetting;
    @Autowired
    private DynamicServerService dynamicServerService;

    @Scheduled(cron = "0/5 * * * * ?")
    public void destroyServerJob() {
        CopyOnWriteArraySet<ServerInfo> servers = liveManSetting.getServers();
        for (ServerInfo server : servers) {
            if (server.getExternalServiceType() != null) {
                if (server.getDateCreated() == null) {
                    server.setDateCreated(System.currentTimeMillis());
                }
                int dMinutes = new Date().getMinutes() - new Date(server.getDateCreated()).getMinutes();
                // 在1分钟内就要进入下一个收费周期了，检查是否需要释放服务器
                if (dMinutes == -1) {
                    if (server.getCurrentVideo() == null) {
                        servers.remove(server);
                        dynamicServerService.destroy(server);
                    }
                }
            }
        }
        settingConfig.saveSetting(liveManSetting);
    }

    @Scheduled(cron = "0/5 * * * * ?")
    public void accountBillJob() {
        CopyOnWriteArraySet<AccountInfo> accounts = liveManSetting.getAccounts();
        for (AccountInfo account : accounts) {
            Map<Integer, Long> billTimeMap = account.getBillTimeMap();
            for (Integer performance : billTimeMap.keySet()) {
                // 1小时的服务已到期
                if (System.currentTimeMillis() - billTimeMap.get(performance) > 59 * 60 * 1000) {
                    billTimeMap.remove(performance);
                }
            }
        }
        CopyOnWriteArraySet<ServerInfo> servers = liveManSetting.getServers();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (ServerInfo server : servers) {
            VideoInfo currentVideo = server.getCurrentVideo();
            if (currentVideo != null) {
                BroadcastTask broadcastTask = currentVideo.getBroadcastTask();
                VideoInfo videoInfo = broadcastTask.getVideoInfo();
                AccountInfo broadcastAccount = broadcastTask.getBroadcastAccount();
                VideoResolutionEnum broadcastResolution = videoInfo.getCropConf().getBroadcastResolution();
                int serverPoint = liveManSetting.getServerPoints()[broadcastResolution.getPerformance()];
                ConcurrentHashMap<Integer, Long> billTimeMap = broadcastAccount.getBillTimeMap();
                if (!billTimeMap.containsKey(broadcastResolution.getPerformance())) {
                    if (broadcastAccount.getPoint() < serverPoint) {
                        log.info("账户积分不足[roomId=" + broadcastAccount.getRoomId() + ", point=" + broadcastAccount.getPoint() + ", need=" + serverPoint + "]");
                        broadcastTask.terminateTask();
                    } else {
                        String remark = dateFormat.format(new Date()) + " - " + dateFormat.format(new Date(System.currentTimeMillis() + 59 * 60 * 1000)) + " 自动扣费:" + broadcastResolution + "@" + server.getRemark();
                        long result = broadcastAccount.changePoint(-1 * serverPoint, remark);
                        log.info("账户[roomId=" + broadcastAccount.getRoomId() + "]" + remark + ", 扣费:" + serverPoint + ", 剩余:" + result);
                        billTimeMap.put(broadcastResolution.getPerformance(), System.currentTimeMillis());
                    }
                }
            }
        }
        settingConfig.saveSetting(liveManSetting);
    }
}
