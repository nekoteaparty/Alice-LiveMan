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

package site.alice.liveman.customlayout.impl;

import lombok.extern.slf4j.Slf4j;
import site.alice.liveman.model.VideoInfo;

import java.net.MalformedURLException;
import java.net.URL;

@Slf4j
public class DanmakuLayout extends BrowserLayout {
    private String src;
    private float  zoom;

    @Override
    public void setVideoInfo(VideoInfo videoInfo) {
        super.setVideoInfo(videoInfo);
        try {
            width = (int) (width / zoom);
            setUrl(new URL(src + "&zoom=" + zoom));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public float getZoom() {
        return zoom;
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
    }
}
