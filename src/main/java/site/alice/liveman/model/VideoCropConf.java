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

package site.alice.liveman.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import site.alice.liveman.customlayout.CustomLayout;
import site.alice.liveman.jenum.VideoBannedTypeEnum;

import java.io.Serializable;
import java.util.TreeSet;

public class VideoCropConf implements Serializable {
    private VideoBannedTypeEnum   videoBannedType = VideoBannedTypeEnum.NONE;
    private int                   blurSize;
    private TreeSet<CustomLayout> layouts;
    @JsonIgnore
    private byte[]                cachedDrawBytes;
    @JsonIgnore
    private byte[]                cachedBlurBytes;

    public VideoCropConf() {
        layouts = new TreeSet<>();
    }

    public VideoBannedTypeEnum getVideoBannedType() {
        return videoBannedType;
    }

    public void setVideoBannedType(VideoBannedTypeEnum videoBannedType) {
        this.videoBannedType = videoBannedType;
    }

    public int getBlurSize() {
        return blurSize;
    }

    public void setBlurSize(int blurSize) {
        this.blurSize = blurSize;
    }

    public TreeSet<CustomLayout> getLayouts() {
        return layouts;
    }

    public void setLayouts(TreeSet<CustomLayout> layouts) {
        this.layouts = layouts;
    }

    public byte[] getCachedDrawBytes() {
        return cachedDrawBytes;
    }

    public void setCachedDrawBytes(byte[] cachedDrawBytes) {
        this.cachedDrawBytes = cachedDrawBytes;
    }

    public byte[] getCachedBlurBytes() {
        return cachedBlurBytes;
    }

    public void setCachedBlurBytes(byte[] cachedBlurBytes) {
        this.cachedBlurBytes = cachedBlurBytes;
    }

    @Override
    public String toString() {
        return "VideoCropConf{" +
                "videoBannedType=" + videoBannedType +
                ", blurSize=" + blurSize +
                ", layouts=" + layouts +
                '}';
    }
}
