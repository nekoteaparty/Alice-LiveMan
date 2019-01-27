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

import de.tuberlin.onedrivesdk.OneDriveException;
import de.tuberlin.onedrivesdk.OneDriveSDK;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import site.alice.liveman.config.SettingConfig;
import site.alice.liveman.model.AccountInfo;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.utils.OneDriveUtil;
import site.alice.liveman.web.dataobject.ActionResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@Controller
@RequestMapping("/api/onedrive")
public class OneDriveController {

    @Autowired
    private HttpServletResponse response;
    @Autowired
    private HttpSession         session;
    @Autowired
    private LiveManSetting      liveManSetting;
    @Autowired
    private SettingConfig       settingConfig;
    @Autowired
    private OneDriveUtil        oneDriveUtil;

    @RequestMapping("/oauth/callback")
    public String callback(String code) throws Exception {
        OneDriveSDK sdk = oneDriveUtil.getOneDriveSDK(code);
        String refreshToken = sdk.getRefreshToken();
        sdk.authenticateWithRefreshToken(refreshToken);
        liveManSetting.setOneDriveToken(refreshToken);
        settingConfig.saveSetting(liveManSetting);
        return "redirect:" + liveManSetting.getBaseUrl() + "/main/system";
    }

    @RequestMapping("/oauth/clean")
    @ResponseBody
    public ActionResult clean() throws Exception {
        AccountInfo accountInfo = (AccountInfo) session.getAttribute("account");
        if (!accountInfo.isAdmin()) {
            return ActionResult.getErrorResult("只有管理员才能执行此操作");
        }
        liveManSetting.setOneDriveClientSecret(null);
        liveManSetting.setOneDriveClientId(null);
        liveManSetting.setOneDriveToken(null);
        settingConfig.saveSetting(liveManSetting);
        return ActionResult.getSuccessResult(null);
    }

    @RequestMapping("/oauth/config")
    public String config(String clientId, String clientSecret) throws Exception {
        AccountInfo accountInfo = (AccountInfo) session.getAttribute("account");
        if (!accountInfo.isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "只有管理员才能执行此操作");
            return null;
        }
        liveManSetting.setOneDriveClientId(clientId);
        liveManSetting.setOneDriveClientSecret(clientSecret);
        settingConfig.saveSetting(liveManSetting);
        return "redirect:" + oneDriveUtil.getOneDriveSDK().getAuthenticationURL();
    }

    @RequestMapping("/redirectRecord")
    public String redirectRecord() throws IOException, OneDriveException {
        return "redirect:" + oneDriveUtil.getOneFolder("Record").createLink();
    }
}
