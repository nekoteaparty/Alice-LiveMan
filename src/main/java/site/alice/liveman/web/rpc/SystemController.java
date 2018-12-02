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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.alice.liveman.config.SettingConfig;
import site.alice.liveman.model.AccountInfo;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.web.dataobject.ActionResult;
import site.alice.liveman.web.dataobject.vo.SettingVO;

import javax.servlet.http.HttpSession;

@Slf4j
@RestController
@RequestMapping("/api/system")
public class SystemController {

    @Autowired
    private HttpSession    session;
    @Autowired
    private LiveManSetting liveManSetting;
    @Autowired
    private SettingConfig  settingConfig;

    @RequestMapping("/getSetting.json")
    public ActionResult<SettingVO> getSetting() {
        AccountInfo account = (AccountInfo) session.getAttribute("account");
        if (!account.isAdmin()) {
            return ActionResult.getErrorResult("权限不足");
        }
        SettingVO settingVO = new SettingVO();
        settingVO.setFfmpegPath(liveManSetting.getFfmpegPath());
        settingVO.setBannedKeywords(String.join(",", liveManSetting.getBannedKeywords()));
        settingVO.setBannedYoutubeChannel(String.join(",", liveManSetting.getBannedYoutubeChannel()));
        settingVO.setDefaultResolution(liveManSetting.getDefaultResolution());
        return ActionResult.getSuccessResult(settingVO);
    }

    @RequestMapping("/saveSetting.json")
    public ActionResult<SettingVO> saveSetting(@RequestBody SettingVO settingVO) {
        AccountInfo account = (AccountInfo) session.getAttribute("account");
        if (!account.isAdmin()) {
            return ActionResult.getErrorResult("权限不足");
        }
        liveManSetting.setFfmpegPath(settingVO.getFfmpegPath());
        liveManSetting.setBannedKeywords(settingVO.getBannedKeywordsArray());
        liveManSetting.setBannedYoutubeChannel(settingVO.getBannedYoutubeChannelArray());
        liveManSetting.setDefaultResolution(settingVO.getDefaultResolution());
        try {
            settingConfig.saveSetting(liveManSetting);
        } catch (Exception e) {
            log.error("保存系统配置信息失败", e);
            return ActionResult.getErrorResult("系统内部错误，请联系管理员");
        }
        return ActionResult.getSuccessResult(settingVO);
    }
}
