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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.alice.liveman.config.SettingConfig;
import site.alice.liveman.model.AccountInfo;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.service.broadcast.BroadcastServiceManager;
import site.alice.liveman.utils.SecurityUtils;
import site.alice.liveman.web.dataobject.ActionResult;
import site.alice.liveman.web.dataobject.vo.AccountInfoVO;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;

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

    @RequestMapping("/info.json")
    public ActionResult info() {
        AccountInfo account = (AccountInfo) session.getAttribute("account");
        AccountInfoVO accountInfoVO = new AccountInfoVO();
        BeanUtils.copyProperties(account, accountInfoVO);
        if (liveManSetting.findByAccountId(account.getAccountId()) != null) {
            accountInfoVO.setSaved(true);
        }
        return ActionResult.getSuccessResult(accountInfoVO);
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
            log.error("保存系统配置信息失败", e);
            return ActionResult.getErrorResult("系统内部错误，请联系管理员");
        }
        return ActionResult.getSuccessResult(null);
    }

    @RequestMapping("/removeAccount.json")
    public ActionResult removeAccount(String accountId) {
        AccountInfo account = (AccountInfo) session.getAttribute("account");
        AccountInfo byAccountId = liveManSetting.findByAccountId(accountId);
        if (byAccountId != null) {
            if (byAccountId.getAccountId().equals(account.getAccountId()) || account.isAdmin()) {
                if (byAccountId.getCurrentVideo() != null) {
                    BroadcastServiceManager.BroadcastTask broadcastTask = byAccountId.getCurrentVideo().getBroadcastTask();
                    if (broadcastTask != null) {
                        if (!broadcastTask.terminateTask()) {
                            log.info("删除账户信息失败：无法终止转播任务，CAS操作失败");
                            return ActionResult.getErrorResult("删除账户信息失败：无法终止转播任务，请稍后重试");
                        }
                    }
                }
            } else {
                return ActionResult.getErrorResult("您没有权限删除他人账户信息");
            }
        } else {
            return ActionResult.getErrorResult("此账户已被删除，请刷新页面后重试");
        }
        liveManSetting.getAccounts().remove(byAccountId);
        try {
            settingConfig.saveSetting(liveManSetting);
        } catch (Exception e) {
            log.error("保存系统配置信息失败", e);
            return ActionResult.getErrorResult("系统内部错误，请联系管理员");
        }
        return ActionResult.getSuccessResult(null);
    }

    @RequestMapping("/useCard.json")
    public synchronized ActionResult<Integer> useCard(String cards) {
        AccountInfo account = (AccountInfo) session.getAttribute("account");
        if (liveManSetting.findByAccountId(account.getAccountId()) == null) {
            return ActionResult.getErrorResult("请先前往[账户管理]菜单保存账号!");
        }
        int totalPoint = 0;
        try {
            File usedcardFile = new File("usedcard.txt");
            List<String> usedCardLine = Collections.emptyList();
            if (usedcardFile.exists()) {
                usedCardLine = IOUtils.readLines(new FileInputStream(usedcardFile), "utf-8");
            } else {
                usedcardFile.createNewFile();
            }
            Set<String> cardLines = new HashSet<>(Arrays.asList(cards.split("\n")));
            for (String cardLine : cardLines) {
                cardLine = cardLine.trim();
                if (StringUtils.isNotEmpty(cardLine) && !usedCardLine.contains(cardLine)) {
                    try {
                        String decodeCardLine = SecurityUtils.aesDecrypt(cardLine);
                        String[] cardInfo = decodeCardLine.split("\\|");
                        int point = Integer.parseInt(cardInfo[0]);
                        account.changePoint(point, "卡号充值");
                        totalPoint += point;
                        log.info("账户[roomId=" + account.getRoomId() + "]卡号[" + cardLine + "]充值[" + decodeCardLine + "]");
                        IOUtils.write(cardLine + "\n", new FileOutputStream(usedcardFile, true), "utf-8");
                        settingConfig.saveSetting(liveManSetting);
                    } catch (Throwable e) {
                        log.error("账户充值发生错误[roomId=" + account.getRoomId() + ", cardLine=" + cardLine + "]", e);
                        return ActionResult.getErrorResult("处理卡号[" + cardLine + "]时出现错误，请检查卡号是否正确。");
                    }
                }
            }
            return ActionResult.getSuccessResult(totalPoint);
        } catch (Throwable e) {
            log.error("账户充值未知错误[roomId=" + account.getRoomId() + ", cards=" + cards + "]", e);
            return ActionResult.getErrorResult("操作失败，请联系管理员!");
        }
    }

    @RequestMapping("/editAccount.json")
    public ActionResult editAccount(@RequestBody AccountInfoVO accountInfoVO) {
        AccountInfo account = (AccountInfo) session.getAttribute("account");
        AccountInfo byAccountId = liveManSetting.findByAccountId(account.getAccountId());
        if (byAccountId != null) {
            byAccountId.setDescription(accountInfoVO.getDescription());
            byAccountId.setJoinAutoBalance(accountInfoVO.isJoinAutoBalance());
            byAccountId.setPostBiliDynamic(accountInfoVO.isPostBiliDynamic());
            byAccountId.setAutoRoomTitle(accountInfoVO.isAutoRoomTitle());
        } else {
            return ActionResult.getErrorResult("尝试编辑的账户不存在，请刷新页面后重试");
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
