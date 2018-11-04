/*
 * <Alice LiveMan>
 * Copyright (C) <2018>  <NekoSunflower>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package site.alice.liveman.service.live;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.utils.HttpRequestUtil;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AbemaTvLiveService extends LiveService {

    /**
     * TODO 如何获取这个票据？这里的混淆过于强壮了<br/>https://api.abema.io/v1/users<br/>Req:{"deviceId":"be2b3830-951c-4826-b716-4bf3d2e3df61","applicationKeySecret":"QNO3Do1X06BMQ7tFW-2Ex4aNFT_LSYpdWi6mfeFHDjw"}<br/>Res:{"profile":{"userId":"8oERbLifmKKkeb","createdAt":1540990620},"token":"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJkZXYiOiJiZTJiMzgzMC05NTFjLTQ4MjYtYjcxNi00YmYzZDJlM2RmNjEiLCJleHAiOjIxNDc0ODM2NDcsImlzcyI6ImFiZW1hLmlvL3YxIiwic3ViIjoiOG9FUmJMaWZtS0trZWIifQ.bV1i84i9ydm9hS949mjahxzRDQFyXMiUnHILvkEs0fs"}
     */
    private static final String       bearer         = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJkZXYiOiJhN2EyZjZiOS0zM2QyLTQ2OWEtODUwMS1lNTkyZjUzNDk3NDEiLCJleHAiOjIxNDc0ODM2NDcsImlzcyI6ImFiZW1hLmlvL3YxIiwic3ViIjoiOG5WY0QydlN5REZObmoifQ.CP3TDvTqKDWt-r8bJfsPevSZeax24xZksoLmg6hJOYE";
    private static final String       userId         = "8nVcD2vSyDFNnj";
    private static final Pattern      channelPattern = Pattern.compile("https://abema.tv/channels/(.+?)/slots/(.+)");
    private static final Proxy        JAPAN_PROXY    = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 8002));
    private static final Pattern      m3u8KeyPattern = Pattern.compile("#EXT-X-KEY:METHOD=(.+?),URI=\"abematv-license://(.+?)\",IV=0x(.+)");
    private static final ScriptEngine scriptEngine;

    static {
        ScriptEngineManager manager = new ScriptEngineManager();
        scriptEngine = manager.getEngineByName("javascript");
        try {
            ClassPathResource resource = new ClassPathResource("abematv.js");
            scriptEngine.eval(String.join("", IOUtils.readLines(resource.getInputStream())));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] getDecodeKey(String cid, String k) throws ScriptException {
        ScriptObjectMirror eval = (ScriptObjectMirror) scriptEngine.eval("getDecodeKey(\"" + cid + "\",\"" + userId + "\",\"" + k + "\")");
        Integer[] toArray = eval.values().toArray(new Integer[0]);
        byte[] key = new byte[toArray.length];
        for (int i = 0; i < toArray.length; i++) {
            key[i] = toArray[i].byteValue();
        }
        return key;
    }

    @Override
    protected VideoInfo getLiveVideoInfo(ChannelInfo channelInfo) throws Exception {
        String channelUrl = channelInfo.getChannelUrl();
        Matcher matcher = channelPattern.matcher(channelUrl);
        if (matcher.find()) {
            String channelId = matcher.group(1);
            String slotId = matcher.group(2);
            Map<String, String> requestProperties = new HashMap<>();
            requestProperties.put("Authorization", "bearer " + bearer);
            String slotInfo = HttpRequestUtil.downloadUrl(new URL("https://api.abema.io/v1/media/slots/" + slotId), null, requestProperties, StandardCharsets.UTF_8, JAPAN_PROXY);
            JSONObject slotInfoObj = JSON.parseObject(slotInfo);
            String seriesId = slotInfoObj.getJSONObject("slot").getJSONArray("programs").getJSONObject(0).getJSONObject("series").getString("id");
            Calendar japanCalendar = Calendar.getInstance(TimeZone.getTimeZone("JST"));
            DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            dateFormat.setCalendar(japanCalendar);
            long currentTimeMillis = System.currentTimeMillis();
            String formattedDate = dateFormat.format(currentTimeMillis);
            String timetableInfo = HttpRequestUtil.downloadUrl(new URL(String.format("https://api.abema.io/v1/media?dateFrom=%s&dateTo=%s&channelIds=%s", formattedDate, formattedDate, channelId)), null, requestProperties, StandardCharsets.UTF_8, JAPAN_PROXY);
            JSONArray channelSlots = JSON.parseObject(timetableInfo).getJSONArray("channelSchedules").getJSONObject(0).getJSONArray("slots");
            for (int i = 0; i < channelSlots.size(); i++) {
                JSONObject channelSlot = channelSlots.getJSONObject(i);
                if (channelSlot.getJSONArray("programs").getJSONObject(0).getJSONObject("series").getString("id").equals(seriesId)) {
                    long startAt = channelSlot.getLongValue("startAt") * 1000;
                    long endAt = channelSlot.getLongValue("endAt") * 1000;
                    String videoTitle = channelSlot.getString("title");
                    String videoId = channelSlot.getString("id");
                    if (currentTimeMillis > startAt && currentTimeMillis < endAt) {
                        // 在节目播出时间内
                        String tokenJSON = HttpRequestUtil.downloadUrl(new URL("https://api.abema.io/v1/media/token?osName=pc&osVersion=1.0.0&osLang=&osTimezone=&appVersion=v18.1025.2"), null, requestProperties, StandardCharsets.UTF_8, JAPAN_PROXY);
                        String token = JSON.parseObject(tokenJSON).getString("token");
                        String mediaUrl = "https://linear-abematv.akamaized.net/channel/" + channelId + "/720/playlist.m3u8?ccf=0&kg=486";
                        String m3u8File = HttpRequestUtil.downloadUrl(new URL(mediaUrl), StandardCharsets.UTF_8, JAPAN_PROXY);
                        Matcher keyMatcher = m3u8KeyPattern.matcher(m3u8File);
                        if (keyMatcher.find()) {
                            String lt = keyMatcher.group(2);
                            byte[] iv = Hex.decodeHex(keyMatcher.group(3));
                            String licenseJson = HttpRequestUtil.downloadUrl(new URL("https://license.abema.io/abematv-hls?t=" + token), null, "{\"lt\":\"" + lt + "\",\"kv\":\"wd\",\"kg\":486}", StandardCharsets.UTF_8, JAPAN_PROXY);
                            String cid = JSON.parseObject(licenseJson).getString("cid");
                            String k = JSON.parseObject(licenseJson).getString("k");
                            VideoInfo videoInfo = new VideoInfo(channelInfo, videoId, videoTitle, new URI(mediaUrl), "m3u8");
                            videoInfo.setEncodeMethod(keyMatcher.group(1));
                            videoInfo.setEncodeKey(getDecodeKey(cid, k));
                            videoInfo.setEncodeIV(iv);
                            videoInfo.setNetworkProxy(JAPAN_PROXY);
                            return videoInfo;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected boolean isMatch(URI channelUrl) {
        return channelPattern.matcher(channelUrl.toString()).find();
    }
}
