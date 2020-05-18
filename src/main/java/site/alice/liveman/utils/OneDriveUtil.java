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

import de.tuberlin.onedrivesdk.OneDriveException;
import de.tuberlin.onedrivesdk.OneDriveFactory;
import de.tuberlin.onedrivesdk.OneDriveSDK;
import de.tuberlin.onedrivesdk.common.ConflictBehavior;
import de.tuberlin.onedrivesdk.common.OneDriveScope;
import de.tuberlin.onedrivesdk.folder.OneFolder;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import site.alice.liveman.model.LiveManSetting;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class OneDriveUtil {

    @Autowired
    private LiveManSetting liveManSetting;

    private              OneDriveSDK            sdk;
    private static final Map<String, OneFolder> oneFolderMap = new HashMap<>();

    public OneDriveSDK getOneDriveSDK() throws IOException, OneDriveException {
        if (sdk == null || StringUtils.isEmpty(liveManSetting.getOneDriveToken())) {
            String redirectUrl = liveManSetting.getBaseUrl() + "/api/onedrive/oauth/callback";
            sdk = OneDriveFactory.createOneDriveSDK(
                    liveManSetting.getOneDriveClientId(), liveManSetting.getOneDriveClientSecret(),
                    redirectUrl,
                    OneDriveScope.FILES_READWRITE_ALL, OneDriveScope.OFFLINE_ACCESS);
        }
        if (!sdk.isAuthenticated() && !StringUtils.isEmpty(liveManSetting.getOneDriveToken())) {
            sdk.authenticateWithRefreshToken(liveManSetting.getOneDriveToken());
        }
        return sdk;
    }

    public OneDriveSDK getOneDriveSDK(String authCode) throws IOException, OneDriveException {
        String redirectUrl = liveManSetting.getBaseUrl() + "/api/onedrive/oauth/callback";
        sdk = OneDriveFactory.createOneDriveSDK(
                liveManSetting.getOneDriveClientId(), liveManSetting.getOneDriveClientSecret(),
                redirectUrl,
                OneDriveScope.FILES_READWRITE_ALL, OneDriveScope.OFFLINE_ACCESS);
        sdk.authenticate(authCode);
        return sdk;
    }

    public OneFolder getOneFolder(String path) throws IOException, OneDriveException {
        if (oneFolderMap.containsKey(path)) {
            return oneFolderMap.get(path);
        } else {
            OneFolder folderByPath = null;
            OneFolder rootFolder = getOneDriveSDK().getRootFolder();
            try {
                folderByPath = rootFolder.createFolder("Record", ConflictBehavior.FAIL);
            } catch (Exception ignore) {

            }
            if (folderByPath == null) {
                folderByPath = sdk.getFolderByPath(path);
            }
            if (folderByPath != null) {
                oneFolderMap.put(path, folderByPath);
            }
            return folderByPath;
        }
    }
}
