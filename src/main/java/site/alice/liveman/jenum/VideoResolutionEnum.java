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
    R1080F60("1080P", 60.0, 2), R1080F30("1080P", 30.0, 2), R720F60("720P", 60.0, 2), R720F30("720P", 30.0, 1), R480("480P", 30.0, 1), R360("360P", 30.0, 2), OTHER("unknown", 0.0, 1);

    private String resolution;
    private Double frameRate;
    private int    performance;

    VideoResolutionEnum(String resolution, Double frameRate, int performance) {
        this.resolution = resolution;
        this.frameRate = frameRate;
        this.performance = performance;
    }

    public String getResolution() {
        return resolution;
    }

    public Double getFrameRate() {
        return frameRate;
    }

    public int getPerformance() {
        return performance;
    }
}
