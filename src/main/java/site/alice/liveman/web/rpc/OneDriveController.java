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
import de.tuberlin.onedrivesdk.OneDriveFactory;
import de.tuberlin.onedrivesdk.OneDriveSDK;
import de.tuberlin.onedrivesdk.common.ConflictBehavior;
import de.tuberlin.onedrivesdk.common.OneDriveScope;
import de.tuberlin.onedrivesdk.drive.OneDrive;
import de.tuberlin.onedrivesdk.folder.OneFolder;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import site.alice.liveman.config.SettingConfig;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.utils.OneDriveUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/api/onedrive")
public class OneDriveController {

    @Autowired
    private HttpServletRequest request;
    @Autowired
    private LiveManSetting     liveManSetting;
    @Autowired
    private SettingConfig      settingConfig;
    @Autowired
    private OneDriveUtil       oneDriveUtil;

    @RequestMapping("/oauth/callback")
    public String callback(String code) throws Exception {
        OneDriveSDK sdk = oneDriveUtil.getOneDriveSDK(code);
        String refreshToken = sdk.getRefreshToken();
        sdk.authenticateWithRefreshToken(refreshToken);
        liveManSetting.setOneDriveToken(refreshToken);
        settingConfig.saveSetting(liveManSetting);
        return "redirect:" + liveManSetting.getBaseUrl() + "/main/system";
    }

    @RequestMapping("/oauth/config")
    public String config(String clientId, String clientSecret) throws Exception {
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
