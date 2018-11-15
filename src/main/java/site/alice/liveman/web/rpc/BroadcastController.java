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

package site.alice.liveman.web.rpc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.alice.liveman.mediaproxy.MediaProxyManager;
import site.alice.liveman.mediaproxy.proxytask.MediaProxyTask;
import site.alice.liveman.model.AccountInfo;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.service.broadcast.BroadcastService;
import site.alice.liveman.service.broadcast.BroadcastServiceManager;
import site.alice.liveman.service.broadcast.BroadcastServiceManager.BroadcastTask;
import site.alice.liveman.service.live.LiveServiceFactory;
import site.alice.liveman.web.dataobject.ActionResult;
import site.alice.liveman.web.dataobject.vo.BroadcastTaskVO;

import javax.servlet.http.HttpSession;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequestMapping("/broadcast")
@RestController
public class BroadcastController {

    @Autowired
    private HttpSession             session;
    @Autowired
    private LiveManSetting          liveManSetting;
    @Autowired
    private LiveServiceFactory      liveServiceFactory;
    @Autowired
    private BroadcastServiceManager broadcastServiceManager;

    @RequestMapping("/taskList.json")
    public ActionResult<List<BroadcastTaskVO>> taskList() {
        List<BroadcastTaskVO> broadcastTaskVOList = new ArrayList<>();
        Map<String, MediaProxyTask> executedProxyTaskMap = MediaProxyManager.getExecutedProxyTaskMap();
        for (MediaProxyTask mediaProxyTask : executedProxyTaskMap.values()) {
            VideoInfo videoInfo = mediaProxyTask.getVideoInfo();
            if (videoInfo != null && videoInfo.getBroadcastTask() != null) {
                AccountInfo broadcastAccount = videoInfo.getBroadcastTask().getBroadcastAccount();
                BroadcastTaskVO broadcastTaskVO = new BroadcastTaskVO();
                broadcastTaskVO.setAccountSite(broadcastAccount.getAccountSite());
                broadcastTaskVO.setNickname(broadcastAccount.getNickname());
                ChannelInfo channelInfo = videoInfo.getChannelInfo();
                if (channelInfo != null) {
                    broadcastTaskVO.setChannelName(channelInfo.getChannelName());
                }
                broadcastTaskVO.setRoomId(broadcastAccount.getRoomId());
                broadcastTaskVO.setVideoId(videoInfo.getVideoId());
                broadcastTaskVO.setVideoTitle(videoInfo.getTitle());
                broadcastTaskVO.setSourceUrl(videoInfo.getMediaUrl().toString());
                broadcastTaskVOList.add(broadcastTaskVO);
            }
        }
        return ActionResult.getSuccessResult(broadcastTaskVOList);
    }

    @RequestMapping("/stopTask.json")
    public ActionResult stopTask(String videoId) {
        AccountInfo account = (AccountInfo) session.getAttribute("account");
        log.info("stopTask()[videoId=" + videoId + "][accountRoomId=" + account.getRoomId() + "]");
        MediaProxyTask mediaProxyTask = MediaProxyManager.getExecutedProxyTaskMap().get(videoId);
        if (mediaProxyTask == null) {
            log.info("此转播任务尚未运行，或已停止[MediaProxyTask不存在][videoId=" + videoId + "]");
            return ActionResult.getErrorResult("此转播任务尚未运行，或已停止");
        }
        BroadcastTask broadcastTask = mediaProxyTask.getVideoInfo().getBroadcastTask();
        if (broadcastTask == null) {
            log.info("此转播任务尚未运行，或已停止[BroadcastTask不存在][videoId=" + videoId + "]");
            return ActionResult.getErrorResult("此转播任务尚未运行，或已停止");
        }
        AccountInfo broadcastAccount = broadcastTask.getBroadcastAccount();
        if (broadcastAccount == null) {
            log.info("此转播任务尚未运行，或已停止[BroadcastAccount不存在][videoId=" + videoId + "]");
            return ActionResult.getErrorResult("此转播任务尚未运行，或已停止");
        }
        if (!broadcastAccount.getRoomId().equals(account.getRoomId())) {
            log.info("您没有权限停止他人直播间的推流任务[videoId=" + videoId + "][broadcastRoomId=" + broadcastAccount.getRoomId() + "]");
            return ActionResult.getErrorResult("你没有权限停止他人直播间的推流任务");
        }
        if (broadcastTask.terminateTask()) {
            return ActionResult.getSuccessResult(null);
        } else {
            log.info("终止转播任务失败：CAS操作失败，请刷新页面后重试[videoId=" + videoId + "]");
            return ActionResult.getErrorResult("终止转播任务失败：CAS操作失败，请刷新页面后重试");
        }
    }

    @RequestMapping("/createTask.json")
    public ActionResult createTask(String videoUrl) {
        AccountInfo account = (AccountInfo) session.getAttribute("account");
        try {
            VideoInfo liveVideoInfo = liveServiceFactory.getLiveService(videoUrl).getLiveVideoInfo(new URI(videoUrl), null);
            Map<String, MediaProxyTask> executedProxyTaskMap = MediaProxyManager.getExecutedProxyTaskMap();
            MediaProxyTask mediaProxyTask = executedProxyTaskMap.get(liveVideoInfo.getVideoId());
            if (mediaProxyTask != null) {
                return ActionResult.getErrorResult("操作失败：此媒体已在推流任务列表中，无法添加");
            }
            BroadcastTask broadcastTask = broadcastServiceManager.createSingleBroadcastTask(liveVideoInfo, account);
            if (broadcastTask != null) {
                mediaProxyTask = MediaProxyManager.createProxy(liveVideoInfo);
                if (mediaProxyTask != null) {
                    return ActionResult.getSuccessResult(null);
                } else {
                    return ActionResult.getErrorResult("操作失败：MediaProxyTask创建失败");
                }
            } else {
                return ActionResult.getErrorResult("操作失败：BroadcastTask创建失败");
            }
        } catch (Exception e) {
            log.error("createTask() failed, videoUrl=" + videoUrl, e);
            if (e instanceof URISyntaxException) {
                return ActionResult.getErrorResult("输入的媒体地址不正确");
            } else {
                return ActionResult.getErrorResult("操作失败：" + e.getMessage());
            }
        }

    }
}
