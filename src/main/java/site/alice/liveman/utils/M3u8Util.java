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

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class M3u8Util {
    static public class StreamInfo {
        private String bandwidth;
        private String codecs;
        private String resolution;
        private Double frameRate;

        public StreamInfo(Map<String, String> propertyMap) {
            this.bandwidth = propertyMap.get("BANDWIDTH");
            this.codecs = propertyMap.get("CODECS");
            this.resolution = propertyMap.get("RESOLUTION");
            try {
                if (propertyMap.containsKey("FRAME-RATE")) {
                    this.frameRate = Double.parseDouble(propertyMap.get("FRAME-RATE"));
                }
            } catch (NumberFormatException ignore) {
            }
        }

        public String getBandwidth() {
            return bandwidth;
        }

        public void setBandwidth(String bandwidth) {
            this.bandwidth = bandwidth;
        }

        public String getCodecs() {
            return codecs;
        }

        public void setCodecs(String codecs) {
            this.codecs = codecs;
        }

        public String getResolution() {
            return resolution;
        }

        public void setResolution(String resolution) {
            this.resolution = resolution;
        }

        public Double getFrameRate() {
            return frameRate;
        }

        public void setFrameRate(Double frameRate) {
            this.frameRate = frameRate;
        }
    }

    public static StreamInfo getStreamInfo(String extXStreamInf) {
        log.info("getStreamInfo() [" + extXStreamInf + "]");
        extXStreamInf = extXStreamInf.replace("#EXT-X-STREAM-INF:", "");
        String[] properties = extXStreamInf.split(",");
        Map<String, String> propertyMap = new HashMap<>();
        String lastPropertyName = null;
        for (String property : properties) {
            String[] split = property.split("=");
            if (split.length > 1) {
                lastPropertyName = split[0];
            }
            propertyMap.put(lastPropertyName, (propertyMap.containsKey(lastPropertyName) ? propertyMap.get(lastPropertyName) + "," : "") + split[split.length - 1]);
        }
        log.info("getStreamInfo() success! " + propertyMap);
        return new StreamInfo(propertyMap);
    }
}
