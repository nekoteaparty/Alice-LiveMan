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

package site.alice.liveman.service;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.videofilter.VideoFilter;

import java.util.ArrayList;
import java.util.List;

@Service
public class VideoFilterService implements ApplicationContextAware {

    private List<VideoFilter> videoFilterList = new ArrayList<>();

    public boolean doFilter(VideoInfo videoInfo) {
        for (VideoFilter videoFilter : videoFilterList) {
            if (!videoFilter.doFilter(videoInfo)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        videoFilterList.addAll(applicationContext.getBeansOfType(VideoFilter.class).values());
    }
}
