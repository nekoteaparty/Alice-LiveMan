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

package site.alice.liveman.service.live.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.service.live.LiveService;
import site.alice.liveman.utils.HttpRequestUtil;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AbemaLiveService extends LiveService {

    @Autowired
    private              LiveManSetting liveManSetting;
    private static final String         bearer           = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJkZXYiOiJhN2EyZjZiOS0zM2QyLTQ2OWEtODUwMS1lNTkyZjUzNDk3NDEiLCJleHAiOjIxNDc0ODM2NDcsImlzcyI6ImFiZW1hLmlvL3YxIiwic3ViIjoiOG5WY0QydlN5REZObmoifQ.CP3TDvTqKDWt-r8bJfsPevSZeax24xZksoLmg6hJOYE";
    private static final String         userId           = "8nVcD2vSyDFNnj";
    private static final Pattern        channelPattern   = Pattern.compile("https://abema.tv/channels/(.+?)/slots/(.+)");
    private static final Pattern        m3u8KeyPattern   = Pattern.compile("#EXT-X-KEY:METHOD=(.+?),URI=\"abematv-license://(.+?)\",IV=0x(.+)");
    private static final String         NOW_ON_AIR_URL   = "https://abema.tv/now-on-air/";
    private static final Pattern        hotfixSetFun     = Pattern.compile("!function\\(\\)\\{var _0x[\\w]+=function\\(\\)\\{var _0x[\\w]+=!!\\[]");
    private static final Pattern        hotfixGetFunName = Pattern.compile("\\);}\\((_0x[\\w]+),(_0x[\\w]+),_0x[\\w]+\\)\\);}");

    private ScriptEngine getScriptEngine() throws IOException, URISyntaxException, ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine scriptEngine = manager.getEngineByName("javascript");
        scriptEngine.eval(getHotfixXhrpJS());
        return scriptEngine;
    }

    private String getHotfixXhrpJS() throws URISyntaxException, IOException {
        String xhrpJS = HttpRequestUtil.downloadUrl(new URI("https://abema.tv/xhrp.js"), StandardCharsets.UTF_8);
        xhrpJS = "var window = {Uint8Array: Uint8Array};" + xhrpJS.replace("data;", "return;");
        Matcher setFunMatcher = hotfixSetFun.matcher(xhrpJS);
        if (setFunMatcher.find()) {
            String group = setFunMatcher.group();
            String hotfixGroup = group.replace("!function()", "var getDecodeKey=function(cid,userId,k)");
            xhrpJS = xhrpJS.replace(group, hotfixGroup);
            Matcher findFunNameMatcher = hotfixGetFunName.matcher(xhrpJS);
            if (findFunNameMatcher.find()) {
                String cidParam = findFunNameMatcher.group(1);
                String userIdParam = findFunNameMatcher.group(2);
                Pattern funNamePattern = Pattern.compile("function (_0x[\\w]+?)\\(" + cidParam + "," + userIdParam);
                Matcher getFunNameMatcher = funNamePattern.matcher(xhrpJS);
                if (getFunNameMatcher.find()) {
                    String funName = getFunNameMatcher.group(1);
                    xhrpJS = xhrpJS.substring(0, findFunNameMatcher.end()) + "return " + funName + "(cid,userId,k);" + xhrpJS.substring(findFunNameMatcher.end());
                    xhrpJS = xhrpJS.substring(0, xhrpJS.length() - 3);
                    return xhrpJS;
                }
            }
        }
        throw new RuntimeException("对[xhrp.js]进行动态补丁失败！");
    }

    private byte[] getDecodeKey(String cid, String k) throws ScriptException, IOException, URISyntaxException {
        ScriptObjectMirror eval = (ScriptObjectMirror) getScriptEngine().eval("getDecodeKey(\"" + cid + "\",\"" + userId + "\",\"" + k + "\")");
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
            String slotInfo = HttpRequestUtil.downloadUrl(new URI("https://api.abema.io/v1/media/slots/" + slotId), channelInfo != null ? channelInfo.getCookies() : null, requestProperties, StandardCharsets.UTF_8);
            JSONObject slotInfoObj = JSON.parseObject(slotInfo);
            String seriesId = slotInfoObj.getJSONObject("slot").getJSONArray("programs").getJSONObject(0).getJSONObject("series").getString("id");
            Calendar japanCalendar = Calendar.getInstance(TimeZone.getTimeZone("JST"));
            DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            dateFormat.setCalendar(japanCalendar);
            long currentTimeMillis = System.currentTimeMillis();
            String formattedDate = dateFormat.format(currentTimeMillis);
            String timetableInfo = HttpRequestUtil.downloadUrl(new URI(String.format("https://api.abema.io/v1/media?dateFrom=%s&dateTo=%s&channelIds=%s", formattedDate, formattedDate, channelId)), channelInfo != null ? channelInfo.getCookies() : null, requestProperties, StandardCharsets.UTF_8);
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
                        channelInfo.setStartAt(startAt);
                        channelInfo.setEndAt(endAt);
                        return new URI(NOW_ON_AIR_URL + channelId);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public VideoInfo getLiveVideoInfo(URI videoInfoUrl, ChannelInfo channelInfo, String resolution) throws Exception {
        if (videoInfoUrl == null) {
            return null;
        }
        String channelId = videoInfoUrl.toString().substring(NOW_ON_AIR_URL.length());
        Map<String, String> requestProperties = new HashMap<>();
        requestProperties.put("Authorization", "bearer " + bearer);
        String tokenJSON = HttpRequestUtil.downloadUrl(new URI("https://api.abema.io/v1/media/token?osName=pc&osVersion=1.0.0&osLang=&osTimezone=&appVersion=v18.1204.3"), channelInfo != null ? channelInfo.getCookies() : null, requestProperties, StandardCharsets.UTF_8);
        String token = JSON.parseObject(tokenJSON).getString("token");
        long kg = getKeyGenerator();
        String mediaUrl = "https://ds-linear-abematv.akamaized.net/channel/" + channelId + "/" + resolution + "/playlist.m3u8?ccf=26&kg=" + kg;
        String m3u8File = HttpRequestUtil.downloadUrl(new URI(mediaUrl), channelInfo != null ? channelInfo.getCookies() : null, Collections.emptyMap(), StandardCharsets.UTF_8);
        Matcher keyMatcher = m3u8KeyPattern.matcher(m3u8File);
        if (keyMatcher.find()) {
            String lt = keyMatcher.group(2);
            byte[] iv = Hex.decodeHex(keyMatcher.group(3));
            String licenseJson = HttpRequestUtil.downloadUrl(new URI("https://license.abema.io/abematv-hls?t=" + token), channelInfo != null ? channelInfo.getCookies() : null, "{\"lt\":\"" + lt + "\",\"kv\":\"wd\",\"kg\":" + kg + "}", StandardCharsets.UTF_8);
            String cid = JSON.parseObject(licenseJson).getString("cid");
            String k = JSON.parseObject(licenseJson).getString("k");
            String slotsJson = HttpRequestUtil.downloadUrl(new URI("https://api.abema.io/v1/broadcast/slots/" + cid), channelInfo != null ? channelInfo.getCookies() : null, Collections.emptyMap(), StandardCharsets.UTF_8);
            JSONObject slotsObj = JSON.parseObject(slotsJson).getJSONObject("slot");
            VideoInfo videoInfo = new VideoInfo(channelInfo, cid, slotsObj.getString("title"), videoInfoUrl, new URI(mediaUrl), "m3u8");
            videoInfo.setEncodeMethod(keyMatcher.group(1));
            videoInfo.setEncodeKey(getDecodeKey(cid, k));
            videoInfo.setEncodeIV(iv);
            return videoInfo;
        }
        return null;
    }

    private long getKeyGenerator() {
        return (System.currentTimeMillis() - 1499270400000L) / 1000 / 60 / 60 / 24 + 1;
    }

    @Override
    protected boolean isMatch(URI channelUrl) {
        return channelPattern.matcher(channelUrl.toString()).find() || channelUrl.toString().startsWith(NOW_ON_AIR_URL);
    }
}
