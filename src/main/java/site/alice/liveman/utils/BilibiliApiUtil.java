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

package site.alice.liveman.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import site.alice.liveman.mediaproxy.proxytask.MediaProxyTask;
import site.alice.liveman.model.AccountInfo;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.service.broadcast.BroadcastServiceManager;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class BilibiliApiUtil {

    private static final String DYNAMIC_POST_API   = "https://api.vc.bilibili.com/dynamic_svr/v1/dynamic_svr/create";
    private static final String DYNAMIC_POST_PARAM = "dynamic_id=0&type=4&rid=0&content=#Vtuber##%s# 正在直播：%s https://live.bilibili.com/%s&at_uids=&ctrl=[]&csrf_token=";

    @Autowired
    private LiveManSetting          liveManSetting;
    @Autowired
    private BroadcastServiceManager broadcastServiceManager;

    public void postDynamic(AccountInfo accountInfo) {
        AccountInfo postAccount = accountInfo;
        VideoInfo videoInfo = accountInfo.getCurrentVideo();
        ChannelInfo channelInfo = videoInfo.getChannelInfo();
        String dynamicPostAccountId = channelInfo.getDynamicPostAccountId();
        if (dynamicPostAccountId != null) {
            AccountInfo byAccountId = liveManSetting.findByAccountId(dynamicPostAccountId);
            if (byAccountId == null) {
                log.warn("频道" + channelInfo.getChannelName() + "指定了动态发送账号[" + dynamicPostAccountId + "]，但此账号不存在，将使用推流账号发送动态");
            } else {
                postAccount = byAccountId;
            }
        }
        if (!postAccount.isPostBiliDynamic()) {
            return;
        }
        Matcher matcher = Pattern.compile("bili_jct=(.{32})").matcher(postAccount.getCookies());
        String csrfToken = "";
        if (matcher.find()) {
            csrfToken = matcher.group(1);
        }
        String postData = null;
        try {
            String broadcastRoomId = broadcastServiceManager.getBroadcastService(accountInfo.getAccountSite()).getBroadcastRoomId(accountInfo);
            postData = String.format(DYNAMIC_POST_PARAM, videoInfo.getChannelInfo().getChannelName(), videoInfo.getTitle(), broadcastRoomId) + csrfToken;
            String res = HttpRequestUtil.downloadUrl(new URI(DYNAMIC_POST_API), postAccount.getCookies(), postData, StandardCharsets.UTF_8);
            JSONObject jsonObject = JSONObject.parseObject(res);
            if (!jsonObject.getString("msg").equals("succ")) {
                log.error("发送B站动态失败[postData=" + postData + "]" + res);
            }
        } catch (Exception ex) {
            log.error("发送B站动态失败[postData=" + postData + "]", ex);
        }
    }
}
