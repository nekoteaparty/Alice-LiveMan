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

package site.alice.liveman.jenum;

public enum VideoResolutionEnum {
    R1080F60(1080, 60, 2), R1080F30(1080, 30, 2), R720F60(720, 60, 2), R720F30(720, 30, 1), R480F30(480, 30, 1);

    private Integer resolution;
    private Integer frameRate;
    private int     performance;

    VideoResolutionEnum(Integer resolution, Integer frameRate, int performance) {
        this.resolution = resolution;
        this.frameRate = frameRate;
        this.performance = performance;
    }

    public Integer getResolution() {
        return resolution;
    }

    public Integer getFrameRate() {
        return frameRate;
    }

    public int getPerformance() {
        return performance;
    }
}
