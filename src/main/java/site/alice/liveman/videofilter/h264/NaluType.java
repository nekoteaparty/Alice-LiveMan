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

package site.alice.liveman.videofilter.h264;

public enum NaluType {
    NALU_TYPE_PADDING, // nothing
    NALU_TYPE_SLICE,
    NALU_TYPE_DPA,
    NALU_TYPE_DPB,
    NALU_TYPE_DPC,
    NALU_TYPE_IDR,
    NALU_TYPE_SEI,
    NALU_TYPE_SPS,
    NALU_TYPE_PPS,
    NALU_TYPE_AUD,
    NALU_TYPE_EOSEQ,
    NALU_TYPE_EOSTREAM,
    NALU_TYPE_FILL,
    OTHER
}
