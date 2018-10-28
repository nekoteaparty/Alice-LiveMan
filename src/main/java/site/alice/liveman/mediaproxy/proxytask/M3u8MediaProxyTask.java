package site.alice.liveman.mediaproxy.proxytask;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import site.alice.liveman.mediaproxy.MediaProxyManager;
import site.alice.liveman.utils.HttpRequestUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class M3u8MediaProxyTask extends MediaProxyTask {

    private static final int                   MAX_RETRY_COUNT      = 20;
    private              long                  NEXT_M3U8_WRITE_TIME = 0;
    private              BlockingQueue<String> downloadQueue        = new LinkedBlockingQueue<>();
    private              AtomicInteger         retry                = new AtomicInteger(0);
    private              int                   lastSeqIndex         = 0;
    private              MediaProxyTask        downloadTask;

    public M3u8MediaProxyTask(String videoId, URI sourceUrl, Proxy proxy) {
        super(videoId, sourceUrl, proxy);
        downloadTask = new MediaProxyTask(getVideoId() + "_DOWNLOAD", null, getProxy()) {
            @Override
            protected void runTask() throws InterruptedException {
                while (!getTerminated()) {
                    String queueData = downloadQueue.poll(1000, TimeUnit.MILLISECONDS);
                    if (queueData != null) {
                        for (int i = 0; i < 3; i++) {
                            try {
                                String[] queueDatas = queueData.split("@");
                                File seqFile = new File(queueDatas[1]);
                                if (!seqFile.exists()) {
                                    HttpRequestUtil.downloadToFile(new URL(queueDatas[0]), seqFile, getProxy());
                                    createM3U8File();
                                    retry.set(0);
                                }
                                break;
                            } catch (Exception e) {
                                log.error(getVideoId() + "出错重试(" + retry.incrementAndGet() + "/" + MAX_RETRY_COUNT + ")次", e);
                                if (e instanceof FileNotFoundException) {
                                    break;
                                }
                            }
                        }
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
        downloadTask.terminate();
        createConcatListFile();
    }

    @Override
    public void runTask() throws InterruptedException {
        MediaProxyManager.runProxy(downloadTask);
        while (retry.get() < MAX_RETRY_COUNT && !getTerminated()) {
            long start = System.currentTimeMillis();
            try {
                log.debug("get m3u8 meta info from " + getSourceUrl());
                String[] m3u8Lines = HttpRequestUtil.downloadUrl(getSourceUrl().toURL(), Charset.defaultCharset(), getProxy()).split("\n");
                int seqCount = 0;
                int readSeqCount = 0;
                int startSeq = 0;
                for (String m3u8Line : m3u8Lines) {
                    if (!m3u8Line.startsWith("#")) {
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
                    log.info(getVideoId() + "没有找到可以下载的片段，重试(" + retry.incrementAndGet() + "/" + MAX_RETRY_COUNT + ")次");
                }
            } catch (Exception e) {
                log.error(getVideoId() + "出错重试(" + retry.incrementAndGet() + "/" + MAX_RETRY_COUNT + ")次", e);
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

    private void createConcatListFile() {
        try {
            File m3u8Path = new File(MediaProxyManager.getTempPath() + "/m3u8/" + getVideoId() + "/");
            m3u8Path.mkdirs();
            File[] seqFiles = m3u8Path.listFiles((dir, name) -> name.endsWith(".ts"));
            if (seqFiles.length > 0) {
                List<Integer> seqList = new LinkedList<>();
                for (File file : seqFiles) {
                    seqList.add(Integer.parseInt(FilenameUtils.getBaseName(file.getName())));
                }
                seqList.sort(Comparator.naturalOrder());

                StringBuilder sb = new StringBuilder();
                for (Integer seq : seqList) {
                    sb.append("file ");
                    sb.append(seq).append(".ts\n");
                }
                File m3u8File = new File(m3u8Path + "/list.txt");
                FileUtils.write(m3u8File, sb);
                String cmdLine = "ffmpeg -f concat -i list.txt -c copy index.mkv";
                FileUtils.write(new File(m3u8Path + "/concat.cmd"), cmdLine);
                FileUtils.write(new File(m3u8Path + "/concat.sh"), cmdLine);
                FileUtils.write(new File(m3u8Path + "/play.cmd"), "ffplay -f concat -i list.txt");
            }
        } catch (Exception e) {
            log.error("创建TS文件列表失败", e);
        }
    }
}
