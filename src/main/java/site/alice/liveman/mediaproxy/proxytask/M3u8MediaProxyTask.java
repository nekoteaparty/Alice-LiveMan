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
package site.alice.liveman.mediaproxy.proxytask;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import site.alice.liveman.mediaproxy.MediaProxyManager;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.utils.HttpRequestUtil;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class M3u8MediaProxyTask extends MediaProxyTask {

    protected static final int                   MAX_RETRY_COUNT      = 20;
    private                long                  NEXT_M3U8_WRITE_TIME = 0;
    private                BlockingQueue<String> downloadQueue        = new LinkedBlockingQueue<>();
    protected              AtomicInteger         retryCount           = new AtomicInteger(0);
    private                int                   lastSeqIndex         = 0;
    private final          MediaProxyTask        downloadTask;

    public M3u8MediaProxyTask(String videoId, URI sourceUrl) {
        super(videoId, sourceUrl);
        downloadTask = new MediaProxyTask(getVideoId() + "_DOWNLOAD", null) {
            @Override
            protected void runTask() throws InterruptedException {
                while (retryCount.get() < MAX_RETRY_COUNT) {
                    String queueData = downloadQueue.poll(1000, TimeUnit.MILLISECONDS);
                    if (queueData != null) {
                        for (int i = 0; i < 3; i++) {
                            try {
                                VideoInfo mediaVideoInfo = M3u8MediaProxyTask.this.getVideoInfo();
                                String[] queueDatas = queueData.split("@");
                                File seqFile = new File(queueDatas[1]);
                                if (!seqFile.exists()) {
                                    if (mediaVideoInfo.getEncodeMethod() == null) {
                                        HttpRequestUtil.downloadToFile(new URI(queueDatas[0]), seqFile);
                                    } else {
                                        seqFile.getParentFile().mkdirs();
                                        byte[] encodedData = HttpRequestUtil.downloadUrl(new URI(queueDatas[0]));
                                        try (FileOutputStream seqFileStream = new FileOutputStream(seqFile)) {
                                            try {
                                                SecretKeySpec sKeySpec = new SecretKeySpec(mediaVideoInfo.getEncodeKey(), "AES");
                                                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                                                IvParameterSpec ivParameterSpec = new IvParameterSpec(mediaVideoInfo.getEncodeIV());
                                                cipher.init(Cipher.DECRYPT_MODE, sKeySpec, ivParameterSpec);
                                                byte[] decodedData = cipher.doFinal(encodedData);
                                                IOUtils.write(decodedData, seqFileStream);
                                            } catch (Throwable e) {
                                                log.warn("媒体数据解密失败{} KEY={},IV={},SEQ={}", e.getMessage(), Hex.encodeHexString(mediaVideoInfo.getEncodeKey()), Hex.encodeHexString(mediaVideoInfo.getEncodeIV()), seqFile);
                                            }
                                        }
                                    }
                                    createM3U8File();
                                    retryCount.set(0);
                                }
                                break;
                            } catch (Throwable e) {
                                log.error(getVideoId() + "出错重试(" + retryCount.incrementAndGet() + "/" + MAX_RETRY_COUNT + ")次", e);
                                if (e instanceof FileNotFoundException) {
                                    break;
                                }
                            }
                        }
                    } else if (M3u8MediaProxyTask.this.getTerminated()) {
                        return;
                    }
                }
            }

            @Override
            protected void terminateTask() {

            }
        };
    }

    @Override
    public void terminateTask() {
        downloadTask.waitForTerminate();
    }

    @Override
    public void runTask() throws InterruptedException {
        MediaProxyManager.runProxy(downloadTask);
        File m3u8File = new File(MediaProxyManager.getTempPath() + "/m3u8/" + getVideoId() + "/index.m3u8");
        m3u8File.delete();
        while (retryCount.get() < MAX_RETRY_COUNT && !getTerminated()) {
            long start = System.currentTimeMillis();
            try {
                log.debug("get m3u8 meta info from " + getSourceUrl());
                String[] m3u8Lines = HttpRequestUtil.downloadUrl(getSourceUrl(), Charset.defaultCharset()).split("\n");
                int seqCount = 0;
                int readSeqCount = 0;
                int startSeq = 0;
                for (String m3u8Line : m3u8Lines) {
                    if (!m3u8Line.startsWith("#") && StringUtils.isNotEmpty(m3u8Line.trim())) {
                        int currentSeqIndex = (startSeq + seqCount);
                        if (currentSeqIndex > lastSeqIndex) {
                            m3u8Line = getSourceUrl().resolve(m3u8Line).toString();
                            String queueData = m3u8Line + "@" + MediaProxyManager.getTempPath() + "/m3u8/" + getVideoId() + "/" + currentSeqIndex + ".ts";
                            if (!downloadQueue.contains(queueData)) {
                                downloadQueue.offer(queueData);
                                lastSeqIndex = currentSeqIndex;
                                readSeqCount++;
                            }
                        }
                        seqCount++;
                    } else {
                        if (m3u8Line.startsWith("#EXT-X-MEDIA-SEQUENCE:")) {
                            startSeq = Integer.parseInt(m3u8Line.split(":")[1]);
                        }
                    }
                }
                if (readSeqCount == 0) {
                    log.info(getVideoId() + "没有找到可以下载的片段，重试(" + retryCount.incrementAndGet() + "/" + MAX_RETRY_COUNT + ")次");
                }
            } catch (Throwable e) {
                log.error(getVideoId() + "出错重试(" + retryCount.incrementAndGet() + "/" + MAX_RETRY_COUNT + ")次", e);
            }
            ChannelInfo channelInfo = getVideoInfo().getChannelInfo();
            if (channelInfo != null) {
                Long endAt = channelInfo.getEndAt();
                if (endAt != null && endAt < System.currentTimeMillis()) {
                    log.info("节目[" + channelInfo.getChannelName() + "]已到结束时间，结束媒体流下载");
                    break;
                }
            }
            Thread.sleep(Math.max(2000 - (System.currentTimeMillis() - start), 0));
        }
    }

    private void createM3U8File() throws IOException {
        if (System.currentTimeMillis() < NEXT_M3U8_WRITE_TIME) {
            return;
        }
        File m3u8Path = new File(MediaProxyManager.getTempPath() + "/m3u8/" + getVideoId() + "/");
        m3u8Path.mkdirs();
        File[] seqFiles = m3u8Path.listFiles((dir, name) -> name.endsWith(".ts"));
        if (seqFiles.length > 0) {
            List<Integer> seqList = new LinkedList<>();
            for (File file : seqFiles) {
                seqList.add(Integer.parseInt(FilenameUtils.getBaseName(file.getName())));
            }
            seqList.sort(Comparator.reverseOrder());
            seqList = seqList.subList(0, Math.min(100, seqList.size()));
            Integer preSeqIndex = null;
            for (Iterator<Integer> iterator = seqList.iterator(); iterator.hasNext(); ) {
                if (preSeqIndex == null) {
                    preSeqIndex = iterator.next();
                } else {
                    Integer seqIndex = iterator.next();
                    if (preSeqIndex - seqIndex != 1) {
                        iterator.remove();
                    } else {
                        preSeqIndex = seqIndex;
                    }
                }
            }
            Collections.reverse(seqList);

            StringBuilder sb = new StringBuilder();
            sb.append("#EXTM3U\n" +
                    "#EXT-X-VERSION:3\n" +
                    "#EXT-X-TARGETDURATION:2\n" +
                    "#EXT-X-MEDIA-SEQUENCE:" + seqList.get(0) + "\n" +
                    "#EXT-X-DISCONTINUITY-SEQUENCE:1\n");
            for (Integer seq : seqList) {
                sb.append("#EXTINF:1.0,\n");
                sb.append(seq).append(".ts\n");
            }
            File m3u8File = new File(m3u8Path + "/index.m3u8");
            FileUtils.write(m3u8File, sb);
            NEXT_M3U8_WRITE_TIME = System.currentTimeMillis() + 250;
        }
    }
}
