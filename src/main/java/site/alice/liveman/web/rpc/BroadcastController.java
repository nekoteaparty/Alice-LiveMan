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
import site.alice.liveman.service.broadcast.BroadcastServiceManager.BroadcastTask;
import site.alice.liveman.web.dataobject.ActionResult;
import site.alice.liveman.web.dataobject.vo.BroadcastTaskVO;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequestMapping("/broadcast")
@RestController
public class BroadcastController {

    @Autowired
    private HttpSession    session;
    @Autowired
    private LiveManSetting liveManSetting;

    @RequestMapping("/taskList.json")
    public ActionResult<List<BroadcastTaskVO>> taskList() {
        List<BroadcastTaskVO> broadcastTaskVOList = new ArrayList<>();
        List<AccountInfo> accounts = liveManSetting.getAccounts();
        for (AccountInfo account : accounts) {
            VideoInfo currentVideo = account.getCurrentVideo();
            if (currentVideo != null) {
                BroadcastTaskVO broadcastTaskVO = new BroadcastTaskVO();
                broadcastTaskVO.setAccountSite(account.getAccountSite());
                broadcastTaskVO.setNickname(account.getNickname());
                ChannelInfo channelInfo = currentVideo.getChannelInfo();
                if (channelInfo != null) {
                    broadcastTaskVO.setChannelName(channelInfo.getChannelName());
                }
                broadcastTaskVO.setRoomId(account.getRoomId());
                broadcastTaskVO.setVideoId(currentVideo.getVideoId());
                broadcastTaskVO.setVideoTitle(currentVideo.getTitle());
                broadcastTaskVO.setSourceUrl(currentVideo.getMediaUrl().toString());
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
}
