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

package site.alice.liveman.videofilter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.utils.DynamicAreaUtil;

import java.io.File;
import java.io.IOException;

@Slf4j
@Component
public class BiliBannedKeywordFilter implements VideoFilter {
    @Autowired
    private LiveManSetting liveManSetting;

    @Override
    public boolean doFilter(VideoInfo videoInfo) {
        String bannedKeyword = null;
        for (String bannedChannel : liveManSetting.getBannedYoutubeChannel()) {
            String[] bannedChannelInfo = bannedChannel.split(":");
            if (bannedChannelInfo[1].equals(videoInfo.getDescription())) {
                bannedKeyword = bannedChannelInfo[0];
                break;
            }
        }
        for (String _bannedKeyword : liveManSetting.getBannedKeywords()) {
            if (StringUtils.containsIgnoreCase(videoInfo.getTitle(), _bannedKeyword) || _bannedKeyword.equals(bannedKeyword)) {
                bannedKeyword = _bannedKeyword;
                break;
            }
        }
        if (bannedKeyword == null) {
            return true;
        }
        videoInfo.setBanned(true);
        return true;
    }
}
