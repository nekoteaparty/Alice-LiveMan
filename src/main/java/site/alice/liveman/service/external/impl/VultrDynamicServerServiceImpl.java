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

package site.alice.liveman.service.external.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.alice.liveman.bo.ExternalAppSecretBO;
import site.alice.liveman.dataobject.ExternalAppSecretDO;
import site.alice.liveman.jenum.ExternalServiceType;
import site.alice.liveman.model.ServerInfo;
import site.alice.liveman.service.external.DynamicServerService;
import site.alice.liveman.utils.HttpRequestUtil;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class VultrDynamicServerServiceImpl implements DynamicServerService {

    private static final String API_SERVER_LIST    = "https://api.vultr.com/v1/server/list";
    private static final String API_SERVER_CREATE  = "https://api.vultr.com/v1/server/create";
    private static final String API_SERVER_DESTROY = "https://api.vultr.com/v1/server/destroy";
    private static final int    OSID               = 362;
    private static final int[]  PLANIDS            = {0, 400, 203};
    private static final int    DCID               = 25;
    private static final String SERVER_LABEL       = "ALICE_VULTR_DYNAMIC";

    @Autowired
    private ExternalAppSecretBO externalAppSecretBO;

    @Override
    public List<ServerInfo> list() {
        List<ServerInfo> list = new ArrayList<>();
        try {
            ExternalAppSecretDO appSecret = externalAppSecretBO.getAppSecret(ExternalServiceType.VULTR_API);
            if (appSecret == null) {
                return null;
            }
            Map<String, String> requestProperties = new HashMap<>();
            requestProperties.put("API-Key", appSecret.getAppKey());
            String listJSON = HttpRequestUtil.downloadUrl(URI.create(API_SERVER_LIST), null, requestProperties, StandardCharsets.UTF_8);
            if (listJSON.equals("[]")) {
                return list;
            }
            JSONObject listMap = JSON.parseObject(listJSON);
            for (String subId : listMap.keySet()) {
                JSONObject subData = listMap.getJSONObject(subId);
                if (SERVER_LABEL.equals(subData.getString("label"))) {
                    ServerInfo serverInfo = new ServerInfo();
                    serverInfo.setAddress(subData.getString("main_ip"));
                    serverInfo.setPassword(subData.getString("default_password"));
                    serverInfo.setPort(22);
                    serverInfo.setUsername("root");
                    serverInfo.setRemark("VULTR_" + subData.getString("SUBID"));
                    serverInfo.setPerformance(subData.getInteger("vcpu_count"));
                    serverInfo.setDateCreated(subData.getDate("date_created").getTime() + 8 * 60 * 60 * 1000);
                    serverInfo.setExternalServiceType(ExternalServiceType.VULTR_API);
                    list.add(serverInfo);
                }
            }
            return list;
        } catch (Throwable e) {
            log.error("request /v1/server/list failed", e);
        }
        return null;
    }

    @Override
    public ServerInfo create(int performance) {
        try {
            ExternalAppSecretDO appSecret = externalAppSecretBO.getAppSecret(ExternalServiceType.VULTR_API);
            if (appSecret == null) {
                return null;
            }
            Map<String, String> requestProperties = new HashMap<>();
            requestProperties.put("API-Key", appSecret.getAppKey());
            String subData = HttpRequestUtil.downloadUrl(URI.create(API_SERVER_CREATE), null, "DCID=" + DCID + "&VPSPLANID=" + PLANIDS[performance] + "&OSID=" + OSID + "&label=" + SERVER_LABEL, requestProperties, StandardCharsets.UTF_8);
            JSONObject data = JSON.parseObject(subData);
            String SUBID = data.getString("SUBID");
            int retry = 0;
            while (retry++ < 120) {
                List<ServerInfo> list = list();
                for (ServerInfo serverInfo : list) {
                    if (serverInfo.getRemark().equals("VULTR_" + SUBID)) {
                        return serverInfo;
                    }
                }
                log.info("waiting for SUBID=" + SUBID + " server created..." + retry);
                Thread.sleep(1000);
            }
            throw new TimeoutException("wait server active over 120 times![SUBID=" + SUBID + "]");
        } catch (Throwable e) {
            log.error("create server failed", e);
        }
        return null;
    }

    @Override
    public ServerInfo update(ServerInfo serverInfo) {
        List<ServerInfo> list = list();
        for (ServerInfo server : list) {
            if (server.getRemark().equals(serverInfo.getRemark())) {
                serverInfo.setAddress(server.getAddress());
                serverInfo.setPassword(server.getPassword());
                serverInfo.setPort(server.getPort());
                serverInfo.setUsername(server.getUsername());
                serverInfo.setPerformance(server.getPerformance());
                serverInfo.setDateCreated(server.getDateCreated());
                return serverInfo;
            }
        }
        return null;
    }

    @Override
    public void destroy(ServerInfo serverInfo) {
        log.info("destroy server " + serverInfo);
        if (serverInfo.getExternalServiceType() == ExternalServiceType.VULTR_API) {
            String SUBID = serverInfo.getRemark().substring("VULTR_".length());
            try {
                ExternalAppSecretDO appSecret = externalAppSecretBO.getAppSecret(ExternalServiceType.VULTR_API);
                if (appSecret != null) {
                    Map<String, String> requestProperties = new HashMap<>();
                    requestProperties.put("API-Key", appSecret.getAppKey());
                    HttpRequestUtil.downloadUrl(URI.create(API_SERVER_DESTROY), null, "SUBID=" + SUBID, requestProperties, StandardCharsets.UTF_8);
                }
            } catch (Throwable e) {
                log.error("request /v1/server/destroy failed", e);
            }
        }
    }
}
