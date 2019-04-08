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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.alice.liveman.bo.ExternalAppSecretBO;
import site.alice.liveman.dataobject.ExternalAppSecretDO;
import site.alice.liveman.model.AccountInfo;
import site.alice.liveman.web.dataobject.ActionResult;

import javax.servlet.http.HttpSession;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/externalApp")
public class ExternalAppController {

    @Autowired
    private HttpSession         session;
    @Autowired
    private ExternalAppSecretBO externalAppSecretBO;

    @RequestMapping("/list.json")
    public ActionResult<List<ExternalAppSecretDO>> list() {
        AccountInfo account = (AccountInfo) session.getAttribute("account");
        if (!account.isAdmin()) {
            return ActionResult.getErrorResult("权限不足");
        }
        return ActionResult.getSuccessResult(externalAppSecretBO.selectForList());
    }

    @RequestMapping("/edit.json")
    public ActionResult edit(@RequestBody ExternalAppSecretDO externalAppSecretDO) {
        AccountInfo account = (AccountInfo) session.getAttribute("account");
        if (!account.isAdmin()) {
            return ActionResult.getErrorResult("权限不足");
        }
        int update = externalAppSecretBO.update(externalAppSecretDO);
        if (update > 0) {
            return ActionResult.getSuccessResult(null);
        } else {
            return ActionResult.getErrorResult("更新失败，找不到记录。[appId:" + externalAppSecretDO.getAppId() + ", type:" + externalAppSecretDO.getType() + "]");
        }
    }

    @RequestMapping("/add.json")
    public ActionResult add(@RequestBody ExternalAppSecretDO externalAppSecretDO) {
        AccountInfo account = (AccountInfo) session.getAttribute("account");
        if (!account.isAdmin()) {
            return ActionResult.getErrorResult("权限不足");
        }
        externalAppSecretBO.insert(externalAppSecretDO);
        return ActionResult.getSuccessResult(null);
    }

    @RequestMapping("/remove.json")
    public ActionResult remove(@RequestBody ExternalAppSecretDO externalAppSecretDO) {
        AccountInfo account = (AccountInfo) session.getAttribute("account");
        if (!account.isAdmin()) {
            return ActionResult.getErrorResult("权限不足");
        }
        externalAppSecretBO.remove(externalAppSecretDO);
        return ActionResult.getSuccessResult(null);
    }

}
