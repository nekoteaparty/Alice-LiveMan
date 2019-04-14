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

package site.alice.liveman.videofilter.aac;

import site.alice.liveman.videofilter.PayloadStruct;

import java.util.Arrays;

public class ADTSStruct extends PayloadStruct {
    public static final byte[] START_CODE = new byte[]{(byte) 0xff, (byte) 0xf0};

    public ProfileEnum   profile;
    public FrequenceEnum frequence;

    public ADTSStruct() {
        startCode = START_CODE;
    }

    @Override
    public String toString() {
        return "ADTSStruct{" +
                "profile=" + profile +
                ", frequence=" + frequence +
                ", len=" + len +
                ", startPos=" + startPos +
                ", startCode=" + Arrays.toString(startCode) +
                '}';
    }
}
