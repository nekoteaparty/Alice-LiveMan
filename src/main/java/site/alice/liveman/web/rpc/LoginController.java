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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.alice.liveman.model.AccountInfo;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.service.broadcast.BroadcastServiceManager;
import site.alice.liveman.web.dataobject.ActionResult;
import site.alice.liveman.web.dataobject.vo.AccountInfoVO;

import javax.servlet.http.HttpSession;

@Slf4j
@RestController
@RequestMapping("/api/login")
public class LoginController {

    @Autowired
    private HttpSession             session;
    @Autowired
    private BroadcastServiceManager broadcastServiceManager;
    @Autowired
    private LiveManSetting          liveManSetting;

    @RequestMapping("/login.json")
    public ActionResult<AccountInfoVO> loginWithBili(@RequestBody AccountInfo accountInfo) {
        try {
            broadcastServiceManager.getBroadcastService(accountInfo.getAccountSite()).getBroadcastRoomId(accountInfo);
            AccountInfo byAccountId;
            if ((byAccountId = liveManSetting.findByAccountId(accountInfo.getAccountId())) != null) {
                accountInfo = byAccountId;
            }
            session.setAttribute("account", accountInfo);
            AccountInfoVO accountInfoVO = new AccountInfoVO();
            BeanUtils.copyProperties(accountInfo, accountInfoVO);
            return ActionResult.getSuccessResult(accountInfoVO);
        } catch (Exception e) {
            log.error("登录失败", e);
            return ActionResult.getErrorResult("登录失败");
        }
    }

    @RequestMapping("/logout.json")
    public ActionResult logout() {
        session.invalidate();
        return ActionResult.getSuccessResult(null);
    }
}
