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

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.alice.liveman.mediaproxy.MediaProxyManager;
import site.alice.liveman.mediaproxy.proxytask.MediaProxyTask;
import site.alice.liveman.model.AccountInfo;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.web.dataobject.vo.LiveNowVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/magictea")
public class MagicTeaController {

    @RequestMapping("/liveNow.json")
    public List<LiveNowVO> liveNow() {
        List<LiveNowVO> liveNowVOS = new ArrayList<>();
        Map<String, MediaProxyTask> executedProxyTaskMap = MediaProxyManager.getExecutedProxyTaskMap();
        for (MediaProxyTask mediaProxyTask : executedProxyTaskMap.values()) {
            VideoInfo videoInfo = mediaProxyTask.getVideoInfo();
            if (videoInfo != null && videoInfo.getChannelInfo() != null) {
                LiveNowVO liveNowVO = new LiveNowVO();
                if (videoInfo.getBroadcastTask() != null) {
                    AccountInfo broadcastAccount = videoInfo.getBroadcastTask().getBroadcastAccount();
                    if (broadcastAccount != null) {
                        liveNowVO.setRoomId(broadcastAccount.getRoomId());
                        liveNowVO.setUid(broadcastAccount.getUid());
                    }
                }
                ChannelInfo channelInfo = videoInfo.getChannelInfo();
                if (channelInfo != null) {
                    liveNowVO.setChannelName(channelInfo.getChannelName());
                }
                liveNowVO.setVideoTitle(videoInfo.getTitle());
                liveNowVOS.add(liveNowVO);
            }
        }
        return liveNowVOS;
    }
}
