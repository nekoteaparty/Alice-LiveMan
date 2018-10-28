package site.alice.liveman.service.live;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.utils.HttpRequestUtil;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class YouTubeLiveService extends LiveService {

    private static final String   LIVE_VIDEO_SUFFIX   = "/videos?view=2&flow=grid";
    private static final String   GET_VIDEO_INFO_URL  = "https://www.youtube.com/watch?v=";
    private static final Proxy    VIDEO_PROXY         = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 1080));
    private static final Proxy    WEB_PROXY           = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 8001));
    private static final Pattern  initDataJsonPattern = Pattern.compile("window\\[\"ytInitialData\"] = (.+?);\n");
    private static final Pattern  hlsvpPattern        = Pattern.compile("\"hlsvp\":\"(.+?)\"");
    private static final Pattern  videoTitlePattern   = Pattern.compile("\\\"title\\\":\\\"(.+?)\\\"");
    @Value("${bili.banned.youtube.channel}")
    private              String[] bannedChannels;

    @Override
    public VideoInfo getLiveVideoInfo(ChannelInfo channelInfo) throws Exception {
        String channelUrl = channelInfo.getChannelUrl();
        URL url = new URL(channelUrl + LIVE_VIDEO_SUFFIX);
        String resHtml = HttpRequestUtil.downloadUrl(url, StandardCharsets.UTF_8, WEB_PROXY);
        Matcher matcher = initDataJsonPattern.matcher(resHtml);
        if (matcher.find()) {
            String initDataJson = matcher.group(1);
            JSONObject jsonObject = JSON.parseObject(initDataJson);
            JSONArray tabs = jsonObject.getJSONObject("contents").getJSONObject("twoColumnBrowseResultsRenderer").getJSONArray("tabs");
            JSONArray gridVideoRender = null;
            for (Object tab : tabs) {
                JSONObject tabObject = (JSONObject) tab;
                JSONObject tabRenderer = tabObject.getJSONObject("tabRenderer");
                if (tabRenderer != null && tabRenderer.getBoolean("selected")) {
                    gridVideoRender = tabRenderer.getJSONObject("content").getJSONObject("sectionListRenderer").getJSONArray("contents").getJSONObject(0).getJSONObject("itemSectionRenderer").getJSONArray("contents").getJSONObject(0).getJSONObject("gridRenderer").getJSONArray("items");
                    break;
                }
            }
            String videoInfoRes = null;
            String videoId = null;
            if (gridVideoRender != null) {
                for (Object reader : gridVideoRender) {
                    JSONObject readerObject = (JSONObject) reader;
                    if (readerObject.toJSONString().contains("BADGE_STYLE_TYPE_LIVE_NOW")) {
                        videoId = readerObject.getJSONObject("gridVideoRenderer").getString("videoId");
                        videoInfoRes = HttpRequestUtil.downloadUrl(new URL(GET_VIDEO_INFO_URL + videoId), StandardCharsets.UTF_8, WEB_PROXY);
                        break;
                    }
                }
            }
            if (videoInfoRes != null) {
                Matcher hlsvpMatcher = hlsvpPattern.matcher(videoInfoRes);
                Matcher videoTitleMatcher = videoTitlePattern.matcher(videoInfoRes);
                String videoTitle = "";
                if (videoTitleMatcher.find()) {
                    videoTitle = videoTitleMatcher.group(1);
                }
                for (String bannedChannel : bannedChannels) {
                    String[] bannedChannelInfo = bannedChannel.split(":");
                    if (videoInfoRes.contains("{\"browseId\":\"" + bannedChannelInfo[1] + "\"}")) {
                        videoTitle += "[" + bannedChannelInfo[0] + "]";
                        break;
                    }
                }
                if (hlsvpMatcher.find()) {
                    String hlsvpUrl = URLDecoder.decode(StringEscapeUtils.unescapeJava(hlsvpMatcher.group(1)), StandardCharsets.UTF_8.name());
                    String[] m3u8List = HttpRequestUtil.downloadUrl(new URL(hlsvpUrl), StandardCharsets.UTF_8, VIDEO_PROXY).split("\n");
                    String mediaUrl = null;
                    for (int i = 0; i < m3u8List.length; i++) {
                        if (m3u8List[i].contains("1280x720")) {
                            mediaUrl = m3u8List[i + 1];
                            break;
                        }
                    }
                    if (mediaUrl == null) {
                        mediaUrl = m3u8List[m3u8List.length - 1];
                    }
                    VideoInfo videoInfo = new VideoInfo(channelInfo, videoId, videoTitle, new URI(mediaUrl), "m3u8");
                    videoInfo.setNetworkProxy(VIDEO_PROXY);
                    return videoInfo;
                } else if (videoInfoRes.contains("LIVE_STREAM_OFFLINE")) {
                    return null;
                } else {
                    throw new RuntimeException("没有找到InitData[" + GET_VIDEO_INFO_URL + videoId + "]");
                }
            }
        } else {
            throw new RuntimeException("没有找到InitData[" + url + "]");
        }
        return null;
    }

    @Override
    public boolean isMatch(URI channelUrl) {
        return channelUrl.getHost().contains("youtube.com");
    }
}
