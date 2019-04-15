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

import org.apache.commons.lang.ArrayUtils;
import site.alice.liveman.videofilter.aac.ADTSStruct;
import site.alice.liveman.videofilter.aac.FrequenceEnum;
import site.alice.liveman.videofilter.aac.ProfileEnum;
import site.alice.liveman.videofilter.h264.NALUStruct;
import site.alice.liveman.videofilter.h264.ReferenceIdc;
import site.alice.liveman.videofilter.h264.UnitType;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.BufferUnderflowException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.List;

public class H264Filter {
    private static MappedByteBuffer mappedByteBuffer;
    private static byte[]           buffer = new byte[1024 * 1024 * 10];

    public static void main(String[] args) throws IOException {
        simplest_h264_parser(new File("C:\\Users\\hasee\\Desktop\\5410.ts"));
    }

    static int FindStartCode2(int startPos, byte[] Buf) {
        if (Buf[startPos] != 0 || Buf[startPos + 1] != 0 || Buf[startPos + 2] != 1) return 0; //0x000001?
        else return 1;
    }

    static int FindStartCode3(int startPos, byte[] Buf) {
        if (Buf[startPos] != 0 || Buf[startPos + 1] != 0 || Buf[startPos + 2] != 0 || Buf[startPos + 3] != 1)
            return 0;//0x00000001?
        else return 1;
    }

    static int GetAnnexbNALU(NALUStruct nalu) throws IOException {
        int pos = 0;
        boolean StartCodeFound;
        int rewind;

        nalu.startCode = NALUStruct.START_CODE3;
        if (mappedByteBuffer.remaining() < 3) {
            return -1;
        }
        mappedByteBuffer.get(buffer, 0, 3);
        pos = 3;
        int info2 = FindStartCode2(0, buffer);
        int info3 = 0;
        if (info2 != 1) {
            if (mappedByteBuffer.remaining() < 1) {
                return -1;
            }
            mappedByteBuffer.get(buffer, 3, 1);
            info3 = FindStartCode3(0, buffer);
            pos = 4;
            if (info3 != 1) {
                nalu.startCode = NALUStruct.START_CODE0;
            } else {
                nalu.startCode = NALUStruct.START_CODE4;
            }
        } else {
            nalu.startCode = NALUStruct.START_CODE3;
        }
        StartCodeFound = false;
        info2 = 0;
        info3 = 0;
        nalu.startPos = mappedByteBuffer.position(); // 第一个是nalu标记
        while (!StartCodeFound) {
            try {
                buffer[pos++] = mappedByteBuffer.get();
                info3 = FindStartCode3(pos - 4, buffer);
                if (info3 != 1)
                    info2 = FindStartCode2(pos - 3, buffer);
                StartCodeFound = (info2 == 1 || info3 == 1);
            } catch (BufferUnderflowException e) {
                nalu.len = (pos - 1) - nalu.startCode.length;
                if (nalu.len >= 0) {
                    nalu.buf = new byte[nalu.len];
                    System.arraycopy(buffer, nalu.startCode.length, nalu.buf, 0, nalu.len);
                }
                if (nalu.startCode.length > 0) {
                    nalu.forbiddenBit = nalu.buf[0] & 0x80; //1 bit
                    int naluPriority = (nalu.buf[0] & 0x60) >> 5;
                    if (naluPriority < ReferenceIdc.values().length) {
                        nalu.referenceIdc = ReferenceIdc.values()[naluPriority]; // 2 bit
                    }
                    int naluType = nalu.buf[0] & 0x1f;
                    if (naluType < UnitType.values().length) {
                        nalu.unitType = UnitType.values()[naluType];// 5 bit
                    }
                }
                return pos - 1;
            }
        }

        // Here, we have found another start code (and read length of startcode bytes more than we should
        // have.  Hence, go back in the file
        rewind = (info3 == 1) ? -4 : -3;

        mappedByteBuffer.position(mappedByteBuffer.position() + rewind);

        // Here the Start code, the complete NALU, and the next start code is in the Buf.
        // The size of Buf is pos, pos+rewind are the number of bytes excluding the next
        // start code, and (pos+rewind)-startCode is the size of the NALU excluding the start code

        nalu.len = (pos + rewind) - nalu.startCode.length;
        if (nalu.len >= 0) {
            nalu.buf = new byte[nalu.len];
            System.arraycopy(buffer, nalu.startCode.length, nalu.buf, 0, nalu.len);
        }
        if (nalu.startCode.length > 0) {
            nalu.forbiddenBit = nalu.buf[0] & 0x80; //1 bit
            int naluPriority = (nalu.buf[0] & 0x60) >> 5;
            if (naluPriority < ReferenceIdc.values().length) {
                nalu.referenceIdc = ReferenceIdc.values()[naluPriority]; // 2 bit
            }
            int naluType = nalu.buf[0] & 0x1f;
            if (naluType < UnitType.values().length) {
                nalu.unitType = UnitType.values()[naluType];// 5 bit
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
        List<PayloadStruct> payloadStructList = new ArrayList<>();
        RandomAccessFile mediaFileStream = new RandomAccessFile(h264File, "rw");
        mappedByteBuffer = mediaFileStream.getChannel().map(MapMode.READ_WRITE, 0, mediaFileStream.length());
        while (true) {
            NALUStruct naluStruct = new NALUStruct();
            if (GetAnnexbNALU(naluStruct) == -1) {
                break;
            }
            if (payloadStructList.isEmpty() || naluStruct.unitType != UnitType.OTHER && naluStruct.unitType != UnitType.NALU_TYPE_PADDING && naluStruct.referenceIdc != ReferenceIdc.OTHER) {
                if (!payloadStructList.isEmpty()) {
                    NALUStruct prevNaluStruct = (NALUStruct) payloadStructList.get(payloadStructList.size() - 1);
                    if (prevNaluStruct.unitType == UnitType.OTHER || prevNaluStruct.referenceIdc == ReferenceIdc.OTHER || prevNaluStruct.unitType == UnitType.NALU_TYPE_PADDING) {
                        byte[] buffer = prevNaluStruct.buf;
                        if (prevNaluStruct.len >= 7) {
                            for (int i = 0; i < prevNaluStruct.len; i++) {
                                //Sync words
                                if ((buffer[i] == (byte) 0xff) && ((buffer[i + 1] & 0xf0) == 0xf0)) {
                                    int size = 0;
                                    size |= ((buffer[i + 3] & 0x03) << 11);     //high 2 bit
                                    size |= buffer[i + 4] << 3;                //middle 8 bit
                                    size |= ((buffer[i + 5] & 0xe0) >> 5);        //low 3bit
                                    if (size > 0) {
                                        int profile = (buffer[i + 2] & 0xC0) >> 6;
                                        int frequence = (buffer[i + 2] & 0x3C) >> 2;
                                        if (frequence < FrequenceEnum.values().length && profile < ProfileEnum.values().length) {
                                            if (i + 7 + size < buffer.length) {
                                                ADTSStruct adtsStruct = new ADTSStruct(new byte[]{buffer[i],
                                                                                                  buffer[i + 1]});
                                                adtsStruct.buf = new byte[size];
                                                System.arraycopy(buffer, i + 7, adtsStruct.buf, 0, size);
                                                adtsStruct.startPos = prevNaluStruct.startPos + i + 7;
                                                adtsStruct.len = size;
                                                adtsStruct.frequence = FrequenceEnum.values()[frequence];
                                                adtsStruct.profile = ProfileEnum.values()[profile];
                                                payloadStructList.add(adtsStruct);
                                                i = i + 7 + size;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                payloadStructList.add(naluStruct);
            } else {
                NALUStruct prevNaluStruct = (NALUStruct) payloadStructList.get(payloadStructList.size() - 1);
                if (prevNaluStruct.unitType == UnitType.OTHER || prevNaluStruct.referenceIdc == ReferenceIdc.OTHER || prevNaluStruct.unitType == UnitType.NALU_TYPE_PADDING) {
                    prevNaluStruct.buf = ArrayUtils.addAll(prevNaluStruct.buf, naluStruct.buf);
                    prevNaluStruct.len = prevNaluStruct.buf.length;
                } else {
                    payloadStructList.add(naluStruct);
                }
            }
        }

        for (PayloadStruct payloadStruct : payloadStructList) {
            System.out.println(payloadStruct);
            if (payloadStruct instanceof NALUStruct) {
                NALUStruct naluStruct = (NALUStruct) payloadStruct;
                if (!(naluStruct.unitType == UnitType.NALU_TYPE_IDR || naluStruct.unitType == UnitType.NALU_TYPE_SLICE)) {
                    continue;
                }
            } else if (payloadStruct instanceof ADTSStruct) {
                continue;
            }
            mappedByteBuffer.position((int) (payloadStruct.startPos + 64)); // 跳过前64个字节
            byte[] buf = payloadStruct.buf;
            for (int i = 64; i < buf.length; i++) {
                mappedByteBuffer.put((byte) (buf[i] ^ 0xac));
            }
        }
        mediaFileStream.close();
        return 0;
    }
}
