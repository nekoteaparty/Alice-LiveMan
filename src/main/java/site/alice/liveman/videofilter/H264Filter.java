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

package site.alice.liveman.videofilter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import site.alice.liveman.videofilter.h264.NALUStruct;
import site.alice.liveman.videofilter.h264.NaluPriority;
import site.alice.liveman.videofilter.h264.NaluType;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class H264Filter {
    private static RandomAccessFile h264bitstream;
    private static int              info2 = 0, info3 = 0;

    public static void main(String[] args) throws IOException {
        simplest_h264_parser(new File("C:\\Users\\hasee\\Desktop\\output.ts"));
    }

    static int FindStartCode2(int startPos, byte[] Buf) {
        if (Buf[startPos + 0] != 0 || Buf[startPos + 1] != 0 || Buf[startPos + 2] != 1) return 0; //0x000001?
        else return 1;
    }

    static int FindStartCode3(int startPos, byte[] Buf) {
        if (Buf[startPos + 0] != 0 || Buf[startPos + 1] != 0 || Buf[startPos + 2] != 0 || Buf[startPos + 3] != 1)
            return 0;//0x00000001?
        else return 1;
    }

    static int GetAnnexbNALU(NALUStruct nalu) throws IOException {
        int pos = 0;
        boolean StartCodeFound;
        int rewind;
        byte[] buffer = new byte[nalu.max_size];

        nalu.startcodeprefix_len = 3;
        if (3 != h264bitstream.read(buffer, 0, 3)) {
            return -1;
        }
        pos = 3;
        info2 = FindStartCode2(0, buffer);
        if (info2 != 1) {
            if (1 != h264bitstream.read(buffer, 3, 1)) {
                return -1;
            }
            info3 = FindStartCode3(0, buffer);
            pos = 4;
            if (info3 != 1) {
                nalu.startcodeprefix_len = 0;
            } else {
                nalu.startcodeprefix_len = 4;
            }
        } else {
            nalu.startcodeprefix_len = 3;
        }
        StartCodeFound = false;
        info2 = 0;
        info3 = 0;
        nalu.start_pos = h264bitstream.getFilePointer();
        while (!StartCodeFound) {
            try {
                buffer[pos++] = h264bitstream.readByte();
                info3 = FindStartCode3(pos - 4, buffer);
                if (info3 != 1)
                    info2 = FindStartCode2(pos - 3, buffer);
                StartCodeFound = (info2 == 1 || info3 == 1);
            } catch (EOFException e) {
                nalu.len = (pos - 1) - nalu.startcodeprefix_len;
                if (nalu.len >= 0) {
                    System.arraycopy(buffer, nalu.startcodeprefix_len, nalu.buf, 0, nalu.len);
                }
                if (nalu.startcodeprefix_len > 0) {
                    nalu.forbidden_bit = nalu.buf[0] & 0x80; //1 bit
                    int naluPriority = (nalu.buf[0] & 0x60) >> 5;
                    if (naluPriority < NaluPriority.values().length) {
                        nalu.nal_reference_idc = NaluPriority.values()[naluPriority]; // 2 bit
                    }
                    int naluType = nalu.buf[0] & 0x1f;
                    if (naluType < NaluType.values().length) {
                        nalu.nal_unit_type = NaluType.values()[naluType];// 5 bit
                    }
                }
                return pos - 1;
            }
        }

        // Here, we have found another start code (and read length of startcode bytes more than we should
        // have.  Hence, go back in the file
        rewind = (info3 == 1) ? -4 : -3;

        h264bitstream.seek(h264bitstream.getFilePointer() + rewind);

        // Here the Start code, the complete NALU, and the next start code is in the Buf.
        // The size of Buf is pos, pos+rewind are the number of bytes excluding the next
        // start code, and (pos+rewind)-startcodeprefix_len is the size of the NALU excluding the start code

        nalu.len = (pos + rewind) - nalu.startcodeprefix_len;
        if (nalu.len >= 0) {
            System.arraycopy(buffer, nalu.startcodeprefix_len, nalu.buf, 0, nalu.len);
        }
        if (nalu.startcodeprefix_len > 0) {
            nalu.forbidden_bit = nalu.buf[0] & 0x80; //1 bit
            int naluPriority = (nalu.buf[0] & 0x60) >> 5;
            if (naluPriority < NaluPriority.values().length) {
                nalu.nal_reference_idc = NaluPriority.values()[naluPriority]; // 2 bit
            }
            int naluType = nalu.buf[0] & 0x1f;
            if (naluType < NaluType.values().length) {
                nalu.nal_unit_type = NaluType.values()[naluType];// 5 bit
            }
        }


        return (pos + rewind);
    }

    /**
     * Analysis H.264 Bitstream
     *
     * @param h264File Location of input H.264 bitstream file.
     */
    static int simplest_h264_parser(File h264File) throws IOException {

        NALUStruct naluStruct;
        int buffersize = 1024 * 1024;
        List<NALUStruct> sliceNalus = new ArrayList<>();
        h264bitstream = new RandomAccessFile(h264File, "rw");
        int data_offset = 0;
        int nal_num = 0;
        System.out.println("-----+---------------- NALU Table ---------------+---------+");
        System.out.println(" NUM |    POS  |       IDC      |      TYPE      |   LEN   |");
        System.out.println("-----+---------+--------+-------+--------+-------+---------+");
        int data_lenth = 0;
        while (true) {
            naluStruct = new NALUStruct();
            naluStruct.max_size = buffersize;
            naluStruct.buf = new byte[buffersize];
            data_lenth = GetAnnexbNALU(naluStruct);
            if (data_lenth == -1) {
                break;
            }
            sliceNalus.add(naluStruct);
            // naluStruct.buf = ArrayUtils.subarray(naluStruct.buf, 0, data_lenth);
            String type_str = "";
            switch (naluStruct.nal_unit_type) {
                case NALU_TYPE_SLICE:
                    type_str = StringUtils.rightPad("SLICE", 15);
                    break;
                case NALU_TYPE_DPA:
                    type_str = StringUtils.rightPad("DPA", 15);
                    break;
                case NALU_TYPE_DPB:
                    type_str = StringUtils.rightPad("DPB", 15);
                    break;
                case NALU_TYPE_DPC:
                    type_str = StringUtils.rightPad("DPC", 15);
                    break;
                case NALU_TYPE_IDR:
                    type_str = StringUtils.rightPad("IDR", 15);
                    break;
                case NALU_TYPE_SEI:
                    type_str = StringUtils.rightPad("SEI", 15);
                    break;
                case NALU_TYPE_SPS:
                    type_str = StringUtils.rightPad("SPS", 15);
                    break;
                case NALU_TYPE_PPS:
                    type_str = StringUtils.rightPad("PPS", 15);
                    break;
                case NALU_TYPE_AUD:
                    type_str = StringUtils.rightPad("AUD", 15);
                    break;
                case NALU_TYPE_EOSEQ:
                    type_str = StringUtils.rightPad("EOSEQ", 15);
                    break;
                case NALU_TYPE_EOSTREAM:
                    type_str = StringUtils.rightPad("EOSTREAM", 15);
                    break;
                case NALU_TYPE_FILL:
                    type_str = StringUtils.rightPad("FILL", 15);
                    break;
                default:
                    type_str = StringUtils.rightPad("OTHER", 15);
            }
            String idc_str = "";
            switch (naluStruct.nal_reference_idc) {
                case NALU_PRIORITY_DISPOSABLE:
                    idc_str = StringUtils.rightPad("DISPOS", 15);
                    break;
                case NALU_PRIRITY_LOW:
                    idc_str = StringUtils.rightPad("LOW", 15);
                    break;
                case NALU_PRIORITY_HIGH:
                    idc_str = StringUtils.rightPad("HIGH", 15);
                    break;
                case NALU_PRIORITY_HIGHEST:
                    idc_str = StringUtils.rightPad("HIGHEST", 15);
                    break;
                default:
                    idc_str = StringUtils.rightPad("OTHER", 15);
            }

            if (!(type_str.startsWith("OTHER") || idc_str.startsWith("OTHER"))) {
                System.out.println(String.format("%5d| %8d| %7s| %6s| %8d|", nal_num++, data_offset, idc_str, type_str, naluStruct.len));
            }
            data_offset = data_offset + data_lenth;
        }
        File outputFile = new File("C:\\Users\\hasee\\Desktop\\overframe.ts");
        int i = 0;
        try (OutputStream os = new FileOutputStream(outputFile)) {
            for (NALUStruct nalus : sliceNalus) {
                if (nalus.startcodeprefix_len == 3) {
                    os.write(new byte[]{0, 0, 1});
                } else if (nalus.startcodeprefix_len == 4) {
                    os.write(new byte[]{0, 0, 0, 1});
                }
                os.write(nalus.buf, 0, nalus.len);
            }
        }
        return 0;
    }
}
