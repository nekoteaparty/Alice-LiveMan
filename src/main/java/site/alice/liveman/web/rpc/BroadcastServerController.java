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

package site.alice.liveman.web.rpc;

import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.alice.liveman.config.SettingConfig;
import site.alice.liveman.model.AccountInfo;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.model.ServerInfo;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.service.BroadcastServerService;
import site.alice.liveman.web.dataobject.ActionResult;
import site.alice.liveman.web.dataobject.vo.ServerVO;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@RestController
@RequestMapping("/api/server")
public class BroadcastServerController {

    @Autowired
    private LiveManSetting         liveManSetting;
    @Autowired
    private SettingConfig          settingConfig;
    @Autowired
    private BroadcastServerService broadcastServerService;
    @Autowired
    private HttpSession            session;

    @RequestMapping("/serverList.json")
    public ActionResult<List<ServerVO>> serverList() {
        List<ServerVO> serverVOS = new ArrayList<>();
        CopyOnWriteArraySet<ServerInfo> servers = liveManSetting.getServers();
        for (ServerInfo server : servers) {
            ServerVO serverVO = new ServerVO();
            serverVO.setAddress(server.getAddress());
            serverVO.setRemark(server.getRemark());
            VideoInfo currentVideo = server.getCurrentVideo();
            if (currentVideo != null) {
                serverVO.setVideoId(currentVideo.getVideoId());
                serverVO.setVideoTitle(currentVideo.getTitle());
            }
            serverVOS.add(serverVO);
        }
        return ActionResult.getSuccessResult(serverVOS);
    }

    @RequestMapping("/addServer.json")
    public ActionResult addServer(@RequestBody ServerInfo serverInfo) {
        AccountInfo account = (AccountInfo) session.getAttribute("account");
        if (!account.isAdmin()) {
            return ActionResult.getErrorResult("权限不足");
        }
        if (serverInfo.getRemark() == null) {
            return ActionResult.getErrorResult("服务器标识必填");
        }
        if (liveManSetting.getServers().contains(serverInfo)) {
            return ActionResult.getErrorResult("服务器标识已存在");
        }
        if (serverInfo.getPort() == null) {
            return ActionResult.getErrorResult("SSH端口号必填");
        }
        if (serverInfo.getAddress() == null) {
            return ActionResult.getErrorResult("转播服务器地址必填");
        }
        if (serverInfo.getUsername() == null) {
            return ActionResult.getErrorResult("SSH用户名必填");
        }
        if (serverInfo.getPassword() == null) {
            return ActionResult.getErrorResult("SSH密码必填");
        }
        try {
            broadcastServerService.addServer(serverInfo);
        } catch (Exception e) {
            log.error("添加转播服务器信息失败", e);
            return ActionResult.getErrorResult("系统内部错误，请联系管理员");
        }
        return ActionResult.getSuccessResult(null);
    }

    @RequestMapping("/removeServer.json")
    public ActionResult removeServer(@RequestBody ServerInfo serverInfo) {
        AccountInfo account = (AccountInfo) session.getAttribute("account");
        if (!account.isAdmin()) {
            return ActionResult.getErrorResult("权限不足");
        }
        try {
            liveManSetting.getServers().remove(serverInfo);
            settingConfig.saveSetting(liveManSetting);
        } catch (Exception e) {
            log.error("删除转播服务器信息失败", e);
            return ActionResult.getErrorResult("系统内部错误，请联系管理员");
        }
        return ActionResult.getSuccessResult(null);
    }
}

