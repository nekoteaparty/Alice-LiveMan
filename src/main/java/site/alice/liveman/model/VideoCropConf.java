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

import site.alice.liveman.jenum.VideoBannedTypeEnum;

import java.io.Serializable;

public class VideoCropConf implements Serializable {
    private VideoBannedTypeEnum videoBannedType = VideoBannedTypeEnum.NONE;
    private int                 ctrlWidth;
    private int                 ctrlHeight;
    private int                 ctrlLeft;
    private int                 ctrlTop;

    public VideoBannedTypeEnum getVideoBannedType() {
        return videoBannedType;
    }

    public void setVideoBannedType(VideoBannedTypeEnum videoBannedType) {
        this.videoBannedType = videoBannedType;
    }

    public int getCtrlWidth() {
        return ctrlWidth;
    }

    public void setCtrlWidth(int ctrlWidth) {
        this.ctrlWidth = ctrlWidth;
    }

    public int getCtrlHeight() {
        return ctrlHeight;
    }

    public void setCtrlHeight(int ctrlHeight) {
        this.ctrlHeight = ctrlHeight;
    }

    public int getCtrlLeft() {
        return ctrlLeft;
    }

    public void setCtrlLeft(int ctrlLeft) {
        this.ctrlLeft = ctrlLeft;
    }

    public int getCtrlTop() {
        return ctrlTop;
    }

    public void setCtrlTop(int ctrlTop) {
        this.ctrlTop = ctrlTop;
    }

    @Override
    public String toString() {
        return "VideoCropConf{" +
                "videoBannedType=" + videoBannedType +
                ", ctrlWidth=" + ctrlWidth +
                ", ctrlHeight=" + ctrlHeight +
                ", ctrlLeft=" + ctrlLeft +
                ", ctrlTop=" + ctrlTop +
                '}';
    }
}
