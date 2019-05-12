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

import com.hiczp.bilibili.api.BilibiliAPI;
import com.hiczp.bilibili.api.passport.exception.CaptchaMismatchException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.alice.liveman.config.SettingConfig;
import site.alice.liveman.model.AccountInfo;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.service.broadcast.BroadcastService;
import site.alice.liveman.service.broadcast.BroadcastServiceManager;
import site.alice.liveman.web.dataobject.ActionResult;
import site.alice.liveman.web.dataobject.vo.AccountInfoVO;
import site.alice.liveman.web.dataobject.vo.LoginInfoVO;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/login")
public class LoginController {

    private static final String                  adminRoomId = System.getProperty("admin.room.id");
    @Autowired
    private              HttpSession             session;
    @Autowired
    private              HttpServletResponse     response;
    @Autowired
    private              BroadcastServiceManager broadcastServiceManager;
    @Autowired
    private              LiveManSetting          liveManSetting;
    @Autowired
    private              SettingConfig           settingConfig;

    @RequestMapping("/login.json")
    public ActionResult<AccountInfoVO> loginWithBili(String loginMode, @RequestBody LoginInfoVO loginInfoVO) {
        try {
            AccountInfo accountInfo = new AccountInfo();
            accountInfo.setAccountSite(loginInfoVO.getAccountSite());
            accountInfo.setCookies(loginInfoVO.getCookies());
            BroadcastService broadcastService = broadcastServiceManager.getBroadcastService(accountInfo.getAccountSite());
            if (loginMode.equals("userpwd")) {
                String cookies = broadcastService.getBroadcastCookies(loginInfoVO.getUsername(), loginInfoVO.getPassword(), loginInfoVO.getCaptcha());
                accountInfo.setCookies(cookies);
            }
            broadcastService.getBroadcastRoomId(accountInfo);
            AccountInfoVO accountInfoVO = new AccountInfoVO();
            AccountInfo byAccountId;
            if ((byAccountId = liveManSetting.findByRoomId(accountInfo.getRoomId())) != null) {
                // 更新新的Cookies
                byAccountId.setCookies(accountInfo.getCookies());
                byAccountId.setAccountId(accountInfo.getAccountId());
                byAccountId.setDisable(false);
                accountInfo = byAccountId;
                accountInfoVO.setSaved(true);
                settingConfig.saveSetting(liveManSetting);
            }
            log.info("adminRoomId = '" + adminRoomId + "'");
            accountInfo.setAdmin(accountInfo.getRoomId().equals(adminRoomId));
            session.setAttribute("account", accountInfo);
            BeanUtils.copyProperties(accountInfo, accountInfoVO);
            return ActionResult.getSuccessResult(accountInfoVO);
        } catch (Exception e) {
            log.error("登录失败", e);
            ActionResult<AccountInfoVO> errorResult = ActionResult.getErrorResult("登录失败[ErrMsg:" + e.getMessage() + "]");
            if (e instanceof CaptchaMismatchException) {
                errorResult.setCode(-101);
            }
            return errorResult;
        }
    }

    @RequestMapping("/getCaptcha")
    public void getCaptcha(String accountSite) throws IOException {
        BroadcastService broadcastService = broadcastServiceManager.getBroadcastService(accountSite);
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            IOUtils.copy(broadcastService.getBroadcastCaptcha(), outputStream);
        }
    }

    @RequestMapping("/logout.json")
    public ActionResult logout() {
        session.invalidate();
        return ActionResult.getSuccessResult(null);
    }
}
