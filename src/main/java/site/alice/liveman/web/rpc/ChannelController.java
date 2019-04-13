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

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.alice.liveman.config.SettingConfig;
import site.alice.liveman.model.AccountInfo;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.web.dataobject.ActionResult;
import site.alice.liveman.web.dataobject.vo.ChannelInfoVO;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/channel")
public class ChannelController {

    @Autowired
    private LiveManSetting liveManSetting;
    @Autowired
    private SettingConfig  settingConfig;
    @Autowired
    private HttpSession    session;

    @RequestMapping("/channelList.json")
    public ActionResult<List<ChannelInfoVO>> channelList() {
        Set<ChannelInfo> channels = liveManSetting.getChannels();
        List<ChannelInfoVO> channelInfoVOList = new ArrayList<>();
        for (ChannelInfo channel : channels) {
            ChannelInfoVO channelInfoVO = new ChannelInfoVO();
            BeanUtils.copyProperties(channel, channelInfoVO);
            channelInfoVOList.add(channelInfoVO);
        }
        return ActionResult.getSuccessResult(channelInfoVOList);
    }


    @RequestMapping("/addChannel.json")
    public ActionResult addChannel(@RequestBody ChannelInfo channelInfo) {
        AccountInfo accountInfo = (AccountInfo) session.getAttribute("account");
        if (!accountInfo.isAdmin()) {
            return ActionResult.getErrorResult("只有管理员才能添加频道");
        }
        try {
            Assert.hasText(channelInfo.getChannelName(), "频道名称不能为空");
            Assert.hasText(channelInfo.getChannelUrl(), "频道地址不能为空");
        } catch (IllegalArgumentException e) {
            return ActionResult.getErrorResult(e.getMessage());
        }
        if (!liveManSetting.getChannels().add(channelInfo)) {
            return ActionResult.getErrorResult("尝试添加的频道已存在");
        }
        try {
            settingConfig.saveSetting(liveManSetting);
        } catch (Exception e) {
            log.error("保存系统配置信息失败", e);
            return ActionResult.getErrorResult("系统内部错误，请联系管理员");
        }
        return ActionResult.getSuccessResult(null);
    }

    @RequestMapping("/editChannel.json")
    public ActionResult editChannel(@RequestBody ChannelInfo channelInfo) {
        AccountInfo accountInfo = (AccountInfo) session.getAttribute("account");
        try {
            Assert.hasText(channelInfo.getChannelName(), "频道名称不能为空");
            Assert.hasText(channelInfo.getChannelUrl(), "频道地址不能为空");
        } catch (IllegalArgumentException e) {
            return ActionResult.getErrorResult(e.getMessage());
        }
        log.info("accountId=" + accountInfo.getAccountId() + "编辑频道channelInfo=" + JSON.toJSONString(channelInfo));
        Set<ChannelInfo> channels = liveManSetting.getChannels();
        for (ChannelInfo channel : channels) {
            if (channel.getChannelUrl().equals(channelInfo.getChannelUrl())) {
                channel.setChannelName(channelInfo.getChannelName());
                channel.setDefaultAccountId(channelInfo.getDefaultAccountId());
                channel.setDynamicPostAccountId(channelInfo.getDynamicPostAccountId());
                channel.setAutoBalance(channelInfo.isAutoBalance());
                channel.setDefaultArea(channelInfo.getDefaultArea());
                channel.setNeedRecord(channelInfo.isNeedRecord());
                channel.setDefaultCropConf(channelInfo.getDefaultCropConf());
                if (!StringUtils.isEmpty(channelInfo.getCookies())) {
                    channel.setCookies(channelInfo.getCookies());
                }
                try {
                    settingConfig.saveSetting(liveManSetting);
                } catch (Exception e) {
                    log.error("保存系统配置信息失败", e);
                    return ActionResult.getErrorResult("系统内部错误，请联系管理员");
                }
                return ActionResult.getSuccessResult(null);
            }
        }
        return ActionResult.getErrorResult("没有找到尝试修改的频道记录");
    }

    @RequestMapping("/removeChannel.json")
    public ActionResult removeChannel(@RequestBody ChannelInfo channelInfo) {
        AccountInfo accountInfo = (AccountInfo) session.getAttribute("account");
        if (!accountInfo.isAdmin()) {
            return ActionResult.getErrorResult("只有管理员才能删除频道");
        }
        try {
            Assert.hasText(channelInfo.getChannelName(), "频道名称不能为空");
            Assert.hasText(channelInfo.getChannelUrl(), "频道地址不能为空");
        } catch (IllegalArgumentException e) {
            return ActionResult.getErrorResult(e.getMessage());
        }
        if (!liveManSetting.getChannels().remove(channelInfo)) {
            return ActionResult.getErrorResult("尝试删除的频道不存在，无法删除");
        }
        try {
            settingConfig.saveSetting(liveManSetting);
        } catch (Exception e) {
            log.error("保存系统配置信息失败", e);
            return ActionResult.getErrorResult("系统内部错误，请联系管理员");
        }
        return ActionResult.getSuccessResult(null);
    }
}
