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

package site.alice.liveman.service.live.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.service.live.LiveService;
import site.alice.liveman.utils.HttpRequestUtil;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AbemaTvLiveService extends LiveService {

    private static final String       bearer         = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJkZXYiOiJhN2EyZjZiOS0zM2QyLTQ2OWEtODUwMS1lNTkyZjUzNDk3NDEiLCJleHAiOjIxNDc0ODM2NDcsImlzcyI6ImFiZW1hLmlvL3YxIiwic3ViIjoiOG5WY0QydlN5REZObmoifQ.CP3TDvTqKDWt-r8bJfsPevSZeax24xZksoLmg6hJOYE";
    private static final String       userId         = "8nVcD2vSyDFNnj";
    private static final Pattern      channelPattern = Pattern.compile("https://abema.tv/channels/(.+?)/slots/(.+)");
    private static final Pattern      m3u8KeyPattern = Pattern.compile("#EXT-X-KEY:METHOD=(.+?),URI=\"abematv-license://(.+?)\",IV=0x(.+)");
    private static final String       NOW_ON_AIR_URL = "https://abema.tv/now-on-air/";
    private static final ScriptEngine scriptEngine;

    static {
        ScriptEngineManager manager = new ScriptEngineManager();
        scriptEngine = manager.getEngineByName("javascript");
        try {
            ClassPathResource resource = new ClassPathResource("abematv.js");
            scriptEngine.eval(String.join("", IOUtils.readLines(resource.getInputStream())));
        } catch (Throwable e) {
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
    public URI getLiveVideoInfoUrl(ChannelInfo channelInfo) throws Exception {
        String channelUrl = channelInfo.getChannelUrl();
        Matcher matcher = channelPattern.matcher(channelUrl);
        if (matcher.find()) {
            String channelId = matcher.group(1);
            String slotId = matcher.group(2);
            Map<String, String> requestProperties = new HashMap<>();
            requestProperties.put("Authorization", "bearer " + bearer);
            String slotInfo = HttpRequestUtil.downloadUrl(new URI("https://api.abema.io/v1/media/slots/" + slotId), null, requestProperties, StandardCharsets.UTF_8);
            JSONObject slotInfoObj = JSON.parseObject(slotInfo);
            String seriesId = slotInfoObj.getJSONObject("slot").getJSONArray("programs").getJSONObject(0).getJSONObject("series").getString("id");
            Calendar japanCalendar = Calendar.getInstance(TimeZone.getTimeZone("JST"));
            DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            dateFormat.setCalendar(japanCalendar);
            long currentTimeMillis = System.currentTimeMillis();
            String formattedDate = dateFormat.format(currentTimeMillis);
            String timetableInfo = HttpRequestUtil.downloadUrl(new URI(String.format("https://api.abema.io/v1/media?dateFrom=%s&dateTo=%s&channelIds=%s", formattedDate, formattedDate, channelId)), null, requestProperties, StandardCharsets.UTF_8);
            JSONArray channelSchedules = JSON.parseObject(timetableInfo).getJSONArray("channelSchedules");
            if (channelSchedules.isEmpty()) {
                return null;
            }
            JSONArray channelSlots = channelSchedules.getJSONObject(0).getJSONArray("slots");
            for (int i = 0; i < channelSlots.size(); i++) {
                JSONObject channelSlot = channelSlots.getJSONObject(i);
                if (channelSlot.getJSONArray("programs").getJSONObject(0).getJSONObject("series").getString("id").equals(seriesId)) {
                    long startAt = channelSlot.getLongValue("startAt") * 1000;
                    long endAt = channelSlot.getLongValue("endAt") * 1000;
                    // 在节目播出时间内
                    if (currentTimeMillis > startAt && currentTimeMillis < endAt) {
                        return new URI(NOW_ON_AIR_URL + channelId);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public VideoInfo getLiveVideoInfo(URI videoInfoUrl, ChannelInfo channelInfo) throws Exception {
        if (videoInfoUrl == null) {
            return null;
        }
        String channelId = videoInfoUrl.toString().substring(NOW_ON_AIR_URL.length());
        Map<String, String> requestProperties = new HashMap<>();
        requestProperties.put("Authorization", "bearer " + bearer);
        String tokenJSON = HttpRequestUtil.downloadUrl(new URI("https://api.abema.io/v1/media/token?osName=pc&osVersion=1.0.0&osLang=&osTimezone=&appVersion=v18.1025.2"), null, requestProperties, StandardCharsets.UTF_8);
        String token = JSON.parseObject(tokenJSON).getString("token");
        String mediaUrl = "https://linear-abematv.akamaized.net/channel/" + channelId + "/720/playlist.m3u8?ccf=0&kg=486";
        String m3u8File = HttpRequestUtil.downloadUrl(new URI(mediaUrl), StandardCharsets.UTF_8);
        Matcher keyMatcher = m3u8KeyPattern.matcher(m3u8File);
        if (keyMatcher.find()) {
            String lt = keyMatcher.group(2);
            byte[] iv = Hex.decodeHex(keyMatcher.group(3));
            String licenseJson = HttpRequestUtil.downloadUrl(new URI("https://license.abema.io/abematv-hls?t=" + token), null, "{\"lt\":\"" + lt + "\",\"kv\":\"wd\",\"kg\":486}", StandardCharsets.UTF_8);
            String cid = JSON.parseObject(licenseJson).getString("cid");
            String k = JSON.parseObject(licenseJson).getString("k");
            String slotsJson = HttpRequestUtil.downloadUrl(new URI("https://api.abema.io/v1/broadcast/slots/" + cid), StandardCharsets.UTF_8);
            JSONObject slotsObj = JSON.parseObject(slotsJson);
            VideoInfo videoInfo = new VideoInfo(channelInfo, cid, slotsObj.getString("title"), new URI(mediaUrl), "m3u8");
            videoInfo.setEncodeMethod(keyMatcher.group(1));
            videoInfo.setEncodeKey(getDecodeKey(cid, k));
            videoInfo.setEncodeIV(iv);
            return videoInfo;
        }
        return null;
    }

    @Override
    protected boolean isMatch(URI channelUrl) {
        return channelPattern.matcher(channelUrl.toString()).find() || channelUrl.toString().startsWith(NOW_ON_AIR_URL);
    }
}
