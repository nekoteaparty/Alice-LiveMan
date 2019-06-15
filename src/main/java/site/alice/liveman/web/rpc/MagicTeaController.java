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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.alice.liveman.mediaproxy.MediaProxyManager;
import site.alice.liveman.mediaproxy.proxytask.MediaProxyTask;
import site.alice.liveman.model.AccountInfo;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.web.dataobject.vo.LiveNowVO;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@CrossOrigin
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
                ChannelInfo channelInfo = videoInfo.getChannelInfo();
                if (channelInfo != null) {
                    liveNowVO.setChannelName(channelInfo.getChannelName());
                }
                liveNowVO.setVideoTitle(videoInfo.getTitle());
                if (videoInfo.getBroadcastTask() != null) {
                    AccountInfo broadcastAccount = videoInfo.getBroadcastTask().getBroadcastAccount();
                    if (broadcastAccount != null) {
                        liveNowVO.setRoomId(broadcastAccount.getRoomId());
                        liveNowVO.setUid(broadcastAccount.getUid());
                        liveNowVO.setAccountId(broadcastAccount.getAccountId());
                        liveNowVO.setAccountSite(broadcastAccount.getAccountSite());
                        liveNowVO.setThumbnail("/api/magictea/" + videoInfo.getVideoId() + "/thumbnail.jpg");
                        liveNowVOS.add(liveNowVO);
                    }
                }
            }
        }
        return liveNowVOS;
    }

    @RequestMapping("/{videoId}/thumbnail.jpg")
    public void thumbnail(@PathVariable String videoId, HttpServletResponse response) {
        try {
            Map<String, MediaProxyTask> executedProxyTaskMap = MediaProxyManager.getExecutedProxyTaskMap();
            MediaProxyTask mediaProxyTask = executedProxyTaskMap.get(videoId);
            if (mediaProxyTask != null) {
                MediaProxyTask.KeyFrame keyFrame = mediaProxyTask.getKeyFrame();
                if (keyFrame != null) {
                    BufferedImage scaledKeyFrame = new BufferedImage((int) (keyFrame.getWidth() * (160.0 / keyFrame.getHeight())), 160, BufferedImage.TYPE_INT_RGB);
                    scaledKeyFrame.createGraphics().drawImage(keyFrame.getFrameImage(), 0, 0, scaledKeyFrame.getWidth(), scaledKeyFrame.getHeight(), null);
                    ImageIO.write(scaledKeyFrame, "jpg", response.getOutputStream());
                }
            }
        } catch (Throwable e) {
            log.error("getKeyFrame Failed videoId=" + videoId, e);
        }
    }
}
