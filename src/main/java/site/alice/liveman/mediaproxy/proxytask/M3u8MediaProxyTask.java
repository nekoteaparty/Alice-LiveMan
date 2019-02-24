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
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import site.alice.liveman.mediaproxy.MediaProxyManager;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.utils.FfmpegUtil;
import site.alice.liveman.utils.HttpRequestUtil;
import site.alice.liveman.utils.ProcessUtil;

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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class M3u8MediaProxyTask extends MediaProxyTask {

    protected static final int                        MAX_RETRY_COUNT      = 60;
    private                long                       NEXT_M3U8_WRITE_TIME = 0;
    private                BlockingDeque<M3u8SeqInfo> downloadDeque        = new LinkedBlockingDeque<>();
    protected              AtomicInteger              retryCount           = new AtomicInteger(0);
    private                int                        lastSeqIndex         = 0;
    private final          MediaProxyTask             downloadTask;
    @Autowired
    private                LiveManSetting             liveManSetting;

    public M3u8MediaProxyTask(String videoId, URI sourceUrl) {
        super(videoId, sourceUrl);
        downloadTask = new MediaProxyTask(getVideoId() + "_DOWNLOAD", null) {
            @Override
            protected void runTask() throws InterruptedException {
                VideoInfo mediaVideoInfo = M3u8MediaProxyTask.this.getVideoInfo();
                boolean needLowFrameRate = liveManSetting.getPreReEncode() && mediaVideoInfo.getVideoId().endsWith("_low") && "60".equals(mediaVideoInfo.getFrameRate());
                final BlockingQueue<M3u8SeqInfo> toLowFrameRatePidQueue = new LinkedBlockingQueue<>();
                if (needLowFrameRate) {
                    MediaProxyManager.runProxy(new MediaProxyTask(getVideoId() + "_LOW-FRAME-RATE", null) {
                        @Override
                        protected void runTask() throws Exception {
                            while (!M3u8MediaProxyTask.this.getTerminated()) {
                                M3u8SeqInfo m3u8SeqInfo = toLowFrameRatePidQueue.poll(1000, TimeUnit.MILLISECONDS);
                                if (m3u8SeqInfo != null) {
                                    ProcessUtil.waitProcess(m3u8SeqInfo.getConvertPid());
                                    m3u8SeqInfo.getSeqFile().delete();
                                    createM3U8File();
                                }
                            }
                        }

                        @Override
                        protected void terminateTask() {

                        }
                    });
                }
                while (retryCount.get() < MAX_RETRY_COUNT) {
                    M3u8SeqInfo m3u8SeqInfo = downloadDeque.poll(1000, TimeUnit.MILLISECONDS);
                    if (m3u8SeqInfo != null) {
                        File dictSeqFile = m3u8SeqInfo.getSeqFile();
                        if (needLowFrameRate) {
                            m3u8SeqInfo.setSeqFile(new File(m3u8SeqInfo.getSeqFile().toString() + ".tmp"));
                        }
                        downloadSeqFile(m3u8SeqInfo);
                        if (needLowFrameRate) {
                            long process = ProcessUtil.createProcess(FfmpegUtil.buildToLowFrameRateCmdLine(m3u8SeqInfo.getSeqFile(), dictSeqFile), getVideoId() + "_LOW-FRAME-RATE", false);
                            m3u8SeqInfo.setConvertPid(process);
                            toLowFrameRatePidQueue.offer(m3u8SeqInfo);
                        } else {
                            createM3U8File();
                        }
                    } else if (M3u8MediaProxyTask.this.getTerminated()) {
                        return;
                    }
                }
            }

            private void downloadSeqFile(M3u8SeqInfo m3u8SeqInfo) {
                for (int i = 0; i < 3; i++) {
                    try {
                        VideoInfo mediaVideoInfo = M3u8MediaProxyTask.this.getVideoInfo();
                        if (!m3u8SeqInfo.getSeqFile().exists()) {
                            if (mediaVideoInfo.getEncodeMethod() == null) {
                                HttpRequestUtil.downloadToFile(m3u8SeqInfo.getSeqUrl(), m3u8SeqInfo.getSeqFile());
                            } else {
                                m3u8SeqInfo.getSeqFile().getParentFile().mkdirs();
                                byte[] encodedData = HttpRequestUtil.downloadUrl(m3u8SeqInfo.getSeqUrl());
                                try {
                                    SecretKeySpec sKeySpec = new SecretKeySpec(mediaVideoInfo.getEncodeKey(), "AES");
                                    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                                    IvParameterSpec ivParameterSpec = new IvParameterSpec(mediaVideoInfo.getEncodeIV());
                                    cipher.init(Cipher.DECRYPT_MODE, sKeySpec, ivParameterSpec);
                                    byte[] decodedData = cipher.doFinal(encodedData);
                                    try (FileOutputStream seqFileStream = new FileOutputStream(m3u8SeqInfo.getSeqFile())) {
                                        IOUtils.write(decodedData, seqFileStream);
                                    }
                                } catch (Throwable e) {
                                    log.warn("媒体数据解密失败{} KEY={},IV={},SEQ={}", e.getMessage(), Hex.encodeHexString(mediaVideoInfo.getEncodeKey()), Hex.encodeHexString(mediaVideoInfo.getEncodeIV()), m3u8SeqInfo.getSeqFile());
                                }
                            }
                            retryCount.set(0);
                        }
                        break;
                    } catch (Throwable e) {
                        if (e instanceof FileNotFoundException) {
                            log.warn(getVideoId() + "出错，媒体文件已过期", e);
                            break;
                        }
                        log.error(getVideoId() + "出错重试(" + retryCount.incrementAndGet() + "/" + MAX_RETRY_COUNT + ")次", e);
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
        VideoInfo videoInfo = getVideoInfo();
        File m3u8File = new File(MediaProxyManager.getTempPath() + "/m3u8/" + videoInfo.getVideoUnionId() + "/index.m3u8");
        m3u8File.delete();
        boolean isFirst = true;
        while (retryCount.get() < MAX_RETRY_COUNT && !getTerminated()) {
            ChannelInfo channelInfo = getVideoInfo().getChannelInfo();
            List<M3u8SeqInfo> tempSeqList = new LinkedList<>();
            long start = System.currentTimeMillis();
            try {
                String m3u8Context = HttpRequestUtil.downloadUrl(getSourceUrl(), Charset.defaultCharset());
                String[] m3u8Lines = m3u8Context.split("\n");
                int seqCount = 0;
                int readSeqCount = 0;
                int startSeq = 0;
                for (String m3u8Line : m3u8Lines) {
                    if (!m3u8Line.startsWith("#") && StringUtils.isNotEmpty(m3u8Line.trim())) {
                        int currentSeqIndex = (startSeq + seqCount);
                        if (currentSeqIndex > lastSeqIndex) {
                            M3u8SeqInfo m3u8SeqInfo = new M3u8SeqInfo();
                            m3u8SeqInfo.setSeqUrl(getSourceUrl().resolve(m3u8Line));
                            m3u8SeqInfo.setSeqFile(new File(MediaProxyManager.getTempPath() + "/m3u8/" + videoInfo.getVideoUnionId() + "/" + currentSeqIndex + ".ts"));
                            if (!downloadDeque.contains(m3u8SeqInfo)) {
                                tempSeqList.add(m3u8SeqInfo);
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
                if (isFirst && tempSeqList.size() > 3) {
                    tempSeqList = tempSeqList.subList(tempSeqList.size() - 3, tempSeqList.size());
                    isFirst = false;
                }
                for (M3u8SeqInfo m3u8SeqInfo : tempSeqList) {
                    downloadDeque.offer(m3u8SeqInfo);
                }
                if (readSeqCount == 0) {
                    if ((retryCount.incrementAndGet() + 2) % 3 == 0) {
                        log.info(getVideoId() + "没有找到可以下载的片段，重试(" + retryCount.get() + "/" + MAX_RETRY_COUNT + ")次");
                    }
                } else {
                    retryCount.set(0);
                }
            } catch (Throwable e) {
                log.error(getVideoId() + "出错重试(" + retryCount.incrementAndGet() + "/" + MAX_RETRY_COUNT + ")次", e);
            }
            if (channelInfo != null) {
                Long endAt = channelInfo.getEndAt();
                if (endAt != null && endAt < System.currentTimeMillis()) {
                    log.info("节目[" + channelInfo.getChannelName() + "]已到结束时间，结束媒体流下载");
                    break;
                }
            }
            Thread.sleep(Math.max(500 - (System.currentTimeMillis() - start), 0));
        }
    }

    class M3u8SeqInfo {
        private URI  seqUrl;
        private File seqFile;
        private Long convertPid;

        public URI getSeqUrl() {
            return seqUrl;
        }

        public void setSeqUrl(URI seqUrl) {
            this.seqUrl = seqUrl;
        }

        public File getSeqFile() {
            return seqFile;
        }

        public void setSeqFile(File seqFile) {
            this.seqFile = seqFile;
        }

        public Long getConvertPid() {
            return convertPid;
        }

        public void setConvertPid(Long convertPid) {
            this.convertPid = convertPid;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            M3u8SeqInfo that = (M3u8SeqInfo) o;
            return Objects.equals(seqFile, that.seqFile);
        }

        @Override
        public int hashCode() {
            return Objects.hash(seqFile);
        }
    }

    private void createM3U8File() {
        if (System.currentTimeMillis() < NEXT_M3U8_WRITE_TIME) {
            return;
        }
        VideoInfo videoInfo = getVideoInfo();
        File m3u8Path = new File(MediaProxyManager.getTempPath() + "/m3u8/" + videoInfo.getVideoUnionId() + "/");
        m3u8Path.mkdirs();
        File[] seqFiles = m3u8Path.listFiles((dir, name) -> name.endsWith(".ts"));
        if (seqFiles.length > 0) {
            List<Integer> seqList = new LinkedList<>();
            for (File file : seqFiles) {
                if (file.length() > 0) {
                    try {
                        seqList.add(Integer.parseInt(FilenameUtils.getBaseName(file.getName())));
                    } catch (NumberFormatException ignored) {
                        log.info(file.getName() + " is not a seq file skipped.");
                    }
                }
            }
            seqList.sort(Comparator.reverseOrder());
            seqList = seqList.subList(0, Math.min(100, seqList.size()));
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
            File m3u8FileTemp = new File(m3u8Path + "/index.m3u8.tmp");
            try {
                FileUtils.write(m3u8FileTemp, sb);
                File m3u8File = new File(m3u8Path + "/index.m3u8");
                m3u8File.delete();
                m3u8FileTemp.renameTo(m3u8File);
                NEXT_M3U8_WRITE_TIME = System.currentTimeMillis() + 250;
            } catch (IOException e) {
                log.error("M3U8序列文件写入失败", e);
            }
        }
    }
}
