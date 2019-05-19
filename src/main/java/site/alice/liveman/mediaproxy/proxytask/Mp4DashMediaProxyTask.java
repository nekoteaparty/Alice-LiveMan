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
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import site.alice.liveman.mediaproxy.MediaProxyManager;
import site.alice.liveman.utils.HttpRequestUtil;

import javax.websocket.Session;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class Mp4DashMediaProxyTask extends MediaProxyTask {
    private static final int                         MAX_RETRY_COUNT   = 20;
    private              BlockingDeque<URI>          downloadDeque     = new LinkedBlockingDeque<>();
    private              AtomicInteger               retryCount        = new AtomicInteger(0);
    private transient    BlockingQueue<byte[]>       bufferCache       = new ArrayBlockingQueue<>(20);
    private transient    List<BlockingQueue<byte[]>> bufferedQueueList = new CopyOnWriteArrayList<>();
    private transient    byte[]                      m4sVideoHeader;
    private transient    byte[]                      m4sAudioHeader;
    private              long                        m4sVideoPosition;
    private              long                        m4sAudioPosition;
    private final        MediaProxyTask              downloadTask;

    public byte[] getM4sVideoHeader() {
        return m4sVideoHeader;
    }

    public byte[] getM4sAudioHeader() {
        return m4sAudioHeader;
    }

    public Mp4DashMediaProxyTask(String videoId, URI sourceUrl) {
        super(videoId, sourceUrl);
        downloadTask = new MediaProxyTask(getVideoId() + "_DOWNLOAD", null) {

            @Override
            protected void runTask() throws Exception {
                File m4sPath = new File(Mp4DashMediaProxyTask.this.getTempPath());
                m4sPath.mkdirs();
                File mp4File = new File(m4sPath + "/index.mp4");
                try (FileOutputStream fos = new FileOutputStream(mp4File, true)) {
                    while (!Mp4DashMediaProxyTask.this.getTerminated() && retryCount.get() < MAX_RETRY_COUNT) {
                        URI mediaUrl = downloadDeque.poll(1000, TimeUnit.MILLISECONDS);
                        if (mediaUrl != null) {
                            byte[] message = HttpRequestUtil.downloadUrl(mediaUrl);
                            while (!bufferCache.offer(message)) {
                                bufferCache.poll();
                            }
                            for (BlockingQueue<byte[]> bufferedQueue : bufferedQueueList) {
                                while (!bufferedQueue.offer(message)) {
                                    bufferedQueue.poll();
                                }
                            }
                            try {
                                fos.write(message);
                            } catch (IOException e) {
                                log.error(getVideoId() + "直播流写入失败", e);
                            }
                        }
                    }
                }
            }

            @Override
            protected void afterTerminate() {
                Mp4DashMediaProxyTask.this.terminate();
            }

            @Override
            public String getTempPath() {
                return MediaProxyManager.getTempPath() + "/mp4/" + getVideoId() + "/";
            }
        };
    }

    @Override
    protected void afterTerminate() {
        downloadTask.waitForTerminate();
    }

    @Override
    public String getTempPath() {
        return MediaProxyManager.getTempPath() + "/mp4/" + getVideoId() + "/";
    }

    @Override
    protected void runTask() throws Exception {
        MediaProxyManager.runProxy(downloadTask);
        while (!getTerminated() && retryCount.get() < MAX_RETRY_COUNT) {
            try {
                String mpdXml = HttpRequestUtil.downloadUrl(getSourceUrl(), StandardCharsets.UTF_8);
                Document mpdDoc = DocumentHelper.parseText(mpdXml);
                Node videoAdaptationSet = mpdDoc.selectSingleNode("//AdaptationSet[@mimeType='video/mp4']");
                if (videoAdaptationSet != null) {
                    Node representation = videoAdaptationSet.selectSingleNode("Representation");
                    Node segmentTemplate = videoAdaptationSet.selectSingleNode("SegmentTemplate");
                    List dList = segmentTemplate.selectNodes("SegmentTimeline/@d");
                    String t = segmentTemplate.selectSingleNode("SegmentTimeline/@t").getStringValue();
                    String mediaTemplate = segmentTemplate.selectSingleNode("@media").getStringValue();
                    String initTemplate = segmentTemplate.selectSingleNode("@initialization").getStringValue();
                    String representationID = representation.selectSingleNode("@id").getStringValue();
                    if (m4sVideoHeader == null) {
                        m4sVideoHeader = HttpRequestUtil.downloadUrl(getSourceUrl().resolve(initTemplate.replace("$RepresentationID$", representationID)));
                    }
                    long start = Long.parseLong(t);
                    for (Object ds : dList) {
                        long d = Long.parseLong((String) ds);
                        if (d + start > m4sVideoPosition) {
                            m4sVideoPosition = d + start;
                            downloadDeque.offer(getSourceUrl().resolve(mediaTemplate.replace("$RepresentationID$", representationID).replace("$Time$", String.valueOf(m4sVideoPosition))));
                        }
                    }
                }
                Node audioAdaptationSet = mpdDoc.selectSingleNode("//AdaptationSet[@mimeType='audio/mp4']");
                if (audioAdaptationSet != null) {
                    Node representation = audioAdaptationSet.selectSingleNode("Representation");
                    Node segmentTemplate = audioAdaptationSet.selectSingleNode("SegmentTemplate");
                    List dList = segmentTemplate.selectNodes("SegmentTimeline/@d");
                    String t = segmentTemplate.selectSingleNode("SegmentTimeline/@t").getStringValue();
                    String mediaTemplate = segmentTemplate.selectSingleNode("@media").getStringValue();
                    String initTemplate = segmentTemplate.selectSingleNode("@initialization").getStringValue();
                    String representationID = representation.selectSingleNode("@id").getStringValue();
                    if (m4sAudioHeader == null) {
                        m4sAudioHeader = HttpRequestUtil.downloadUrl(getSourceUrl().resolve(initTemplate.replace("$RepresentationID$", representationID)));
                    }
                    long start = Long.parseLong(t);
                    for (Object ds : dList) {
                        long d = Long.parseLong((String) ds);
                        if (d + start > m4sAudioPosition) {
                            m4sAudioPosition = d + start;
                            downloadDeque.offer(getSourceUrl().resolve(mediaTemplate.replace("$RepresentationID$", representationID).replace("$Time$", String.valueOf(m4sAudioPosition))));
                        }
                    }
                }
            } catch (Exception e) {
                log.error(getVideoId() + "出错重试(" + retryCount.incrementAndGet() + "/" + MAX_RETRY_COUNT + ")次", e);
            }
        }
    }

    public void addBufferedQueue(BlockingQueue<byte[]> bufferedQueue) {
        bufferedQueue.addAll(bufferCache);
        bufferedQueueList.add(bufferedQueue);
    }

    public void removeBufferedQueue(BlockingQueue<byte[]> bufferedQueue) {
        bufferedQueueList.remove(bufferedQueue);
    }
}
