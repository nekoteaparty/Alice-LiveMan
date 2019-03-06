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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import site.alice.liveman.utils.HttpRequestUtil;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class NicoLiveMediaProxyTask extends M3u8MediaProxyTask {
    private transient Session session = null;
    private           URI     webSocketUrl;

    public NicoLiveMediaProxyTask(String videoId, URI sourceUrl) {
        super(videoId, sourceUrl);
        webSocketUrl = sourceUrl;
    }

    @Override
    public void runTask() throws InterruptedException {
        session = connectToNicoLive();
        synchronized (this) {
            this.wait(60000);
        }
        try {
            if (getSourceUrl().getScheme().startsWith("wss")) {
                log.error("无法获取NicoLive直播流地址，媒体代理线程退出[videoId=" + getVideoId() + "]");
                return;
            }
            super.runTask();
        } finally {
            terminate();
            if (session != null) {
                try {
                    session.close();
                } catch (IOException ignored) {

                }
            }
        }
    }

    @Override
    public void setSourceUrl(URI sourceUrl) {
        // Nico的如果已经解析成功不允许修改媒体源
        if (getSourceUrl().getScheme().startsWith("wss")) {
            super.setSourceUrl(sourceUrl);
        }
    }

    private Session connectToNicoLive() {
        String[] pathSplit = webSocketUrl.getPath().split("/");
        String _broadcastId = "";
        for (String aPathSplit : pathSplit) {
            if (StringUtils.isNotEmpty(aPathSplit) && StringUtils.isNumeric(aPathSplit)) {
                _broadcastId = aPathSplit;
                break;
            }
        }
        final String broadcastId = _broadcastId;
        WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();
        ClientEndpointConfig clientEndpointConfig = ClientEndpointConfig.Builder.create().configurator(new ClientEndpointConfig.Configurator() {
            @Override
            public void beforeRequest(Map<String, List<String>> headers) {
                headers.put("Origin", Collections.singletonList("http://live2.nicovideo.jp"));
                headers.put("User-Agent", Collections.singletonList("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.102 Safari/537.36"));
                headers.put("Cookie", Collections.singletonList(getVideoInfo().getChannelInfo().getCookies()));
            }
        }).build();
        try {
            return webSocketContainer.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    final RemoteEndpoint.Basic basicRemote = session.getBasicRemote();
                    session.setMaxBinaryMessageBufferSize(10 * 1024 * 1024);
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            try {
                                if (StringUtils.isNotEmpty(message)) {
                                    JSONObject msgJson = JSON.parseObject(message);
                                    System.out.println(msgJson);
                                    String type = msgJson.getString("type");
                                    switch (String.valueOf(type)) {
                                        case "ping":
                                            basicRemote.sendText("{\"type\":\"pong\",\"body\":{}}");
                                            basicRemote.sendText("{\"type\":\"watch\",\"body\":{\"command\":\"watching\",\"params\":[\"" + broadcastId + "\",\"-1\",\"0\"]}}");
                                            break;
                                        case "watch":
                                            JSONObject body = msgJson.getJSONObject("body");
                                            String command = body.getString("command");
                                            switch (String.valueOf(command)) {
                                                case "currentstream":
                                                    JSONObject currentStream = body.getJSONObject("currentStream");
                                                    String uri = currentStream.getString("uri");
                                                    URI m3u8MasterUri = new URI(uri);
                                                    String m3u8File = HttpRequestUtil.downloadUrl(m3u8MasterUri, StandardCharsets.UTF_8);
                                                    String[] m3u8List = m3u8File.split("\n");
                                                    setSourceUrl(m3u8MasterUri.resolve(m3u8List[m3u8List.length - 1]));
                                                    synchronized (NicoLiveMediaProxyTask.this) {
                                                        NicoLiveMediaProxyTask.this.notify();
                                                    }
                                                    break;
                                                case "disconnect":
                                                    session.close();
                                                    break;
                                            }
                                            break;
                                    }
                                }
                            } catch (Exception e) {
                                log.error("处理WebSocket消息时出错[message=" + message + "]", e);
                            }
                        }
                    });
                    try {
                        basicRemote.sendText("{\"type\":\"watch\",\"body\":{\"command\":\"playerversion\",\"params\":[\"leo\"]}}");
                        basicRemote.sendText("{\"type\":\"watch\",\"body\":{\"command\":\"getpermit\",\"requirement\":{\"broadcastId\":\"" + broadcastId + "\",\"route\":\"\",\"stream\":{\"protocol\":\"hls\",\"requireNewStream\":true,\"priorStreamQuality\":\"super_high\",\"isLowLatency\":false},\"room\":{\"isCommentable\":true,\"protocol\":\"webSocket\"}}}}");
                    } catch (IOException e) {
                        log.error("与NicoLive握手失败", e);
                        try {
                            session.close();
                        } catch (IOException ignored) {

                        }
                    }
                }

                @Override
                public void onClose(Session _session, CloseReason closeReason) {
                    session = null;
                    while (session == null && !getTerminated() && retryCount.get() < MAX_RETRY_COUNT) {
                        log.warn(getVideoId() + "WebSocket连接已断开[" + closeReason + "]，重试(" + retryCount.incrementAndGet() + "/" + MAX_RETRY_COUNT + ")次");
                        session = connectToNicoLive();
                    }
                    if (session == null) {
                        synchronized (NicoLiveMediaProxyTask.this) {
                            NicoLiveMediaProxyTask.this.notify();
                        }
                    }
                }
            }, clientEndpointConfig, webSocketUrl);
        } catch (Throwable e) {
            log.error(getVideoId() + "WebSocket连接失败", e);
        }
        return null;
    }
}
