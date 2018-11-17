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
import site.alice.liveman.config.SettingConfig;
import site.alice.liveman.model.AccountInfo;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.web.dataobject.ActionResult;
import site.alice.liveman.web.dataobject.vo.AccountInfoVO;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/account")
public class AccountController {

    @Autowired
    private LiveManSetting liveManSetting;
    @Autowired
    private SettingConfig  settingConfig;
    @Autowired
    private HttpSession    session;

    @RequestMapping("/accountList.json")
    public ActionResult<List<AccountInfoVO>> accountList() {
        Set<AccountInfo> accounts = liveManSetting.getAccounts();
        List<AccountInfoVO> accountInfoVOList = new ArrayList<>();
        for (AccountInfo account : accounts) {
            AccountInfoVO accountInfoVO = new AccountInfoVO();
            BeanUtils.copyProperties(account, accountInfoVO);
            accountInfoVOList.add(accountInfoVO);
        }
        return ActionResult.getSuccessResult(accountInfoVOList);
    }

    @RequestMapping("/addAccount.json")
    public ActionResult addAccount(@RequestBody AccountInfoVO accountInfoVO) {
        AccountInfo account = (AccountInfo) session.getAttribute("account");
        AccountInfo byAccountId = liveManSetting.findByAccountId(account.getAccountId());
        if (byAccountId != null) {
            return ActionResult.getErrorResult("此账号已存在，如要更新账号信息请删除后重新添加");
        }
        account.setDescription(accountInfoVO.getDescription());
        account.setJoinAutoBalance(accountInfoVO.isJoinAutoBalance());
        Set<AccountInfo> accounts = liveManSetting.getAccounts();
        if (!accounts.add(account)) {
            return ActionResult.getErrorResult("此账号已存在，如要更新账号信息请删除后重新添加");
        }
        try {
            settingConfig.saveSetting(liveManSetting);
        } catch (Exception e) {
            log.error("添加账户信息失败", e);
            return ActionResult.getErrorResult("系统内部错误，请联系管理员");
        }
        return ActionResult.getSuccessResult(null);
    }

    @RequestMapping("/removeAccount.json")
    public ActionResult removeAccount(String accountId) {
        AccountInfo account = (AccountInfo) session.getAttribute("account");
        AccountInfo byAccountId = liveManSetting.findByAccountId(accountId);
        if (byAccountId != null) {
            if (byAccountId.getAccountId().equals(account.getAccountId())) {
                liveManSetting.getAccounts().remove(byAccountId);
            } else {
                return ActionResult.getErrorResult("您没有权限删除他人账户信息");
            }
        }
        try {
            settingConfig.saveSetting(liveManSetting);
        } catch (Exception e) {
            log.error("删除账户信息失败", e);
            return ActionResult.getErrorResult("系统内部错误，请联系管理员");
        }
        return ActionResult.getSuccessResult(null);
    }
}
