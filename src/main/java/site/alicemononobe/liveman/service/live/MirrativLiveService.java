package site.alicemononobe.liveman.service.live;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import site.alicemononobe.liveman.model.ChannelInfo;
import site.alicemononobe.liveman.model.VideoInfo;
import site.alicemononobe.liveman.utils.HttpRequestUtil;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
public class MirrativLiveService extends LiveService {

    @Override
    public VideoInfo getLiveVideoInfo(ChannelInfo channelInfo) throws Exception {
        String channelUrl = channelInfo.getChannelUrl();
        String userId = channelUrl.replace("https://www.mirrativ.com/user/", "").replace("/", "");
        URL liveHistoryUrl = new URL("https://www.mirrativ.com/api/live/live_history?user_id=" + userId + "&page=1");
        String liveHistoryJson = HttpRequestUtil.downloadUrl(liveHistoryUrl, StandardCharsets.UTF_8, null);
        JSONObject liveHistory = JSON.parseObject(liveHistoryJson);
        JSONArray lives = liveHistory.getJSONArray("lives");
        if (!lives.isEmpty()) {
            JSONObject liveObj = lives.getJSONObject(0);
            if (liveObj.getBoolean("is_live")) {
                String videoId = liveObj.getString("live_id");
                String liveDetailJson = HttpRequestUtil.downloadUrl(new URL("https://www.mirrativ.com/api/live/live?live_id=" + videoId), StandardCharsets.UTF_8, null);
                JSONObject liveDetailObj = JSON.parseObject(liveDetailJson);
                String videoTitle = liveDetailObj.getString("title");
                URL m3u8ListUrl = new URL(liveDetailObj.getString("streaming_url_hls"));
                String[] m3u8List = HttpRequestUtil.downloadUrl(m3u8ListUrl, StandardCharsets.UTF_8, null).split("\n");
                String mediaUrl = null;
                for (int i = 0; i < m3u8List.length; i++) {
                    if (m3u8List[i].contains("432x768")) {
                        mediaUrl = m3u8List[i + 1];
                        break;
                    }
                }
                if (mediaUrl == null) {
                    mediaUrl = m3u8List[3];
                }
                return new VideoInfo(channelInfo, videoId, videoTitle, m3u8ListUrl.toURI().resolve(mediaUrl), "m3u8");
            }
        }
        return null;
    }

    @Override
    public boolean isMatch(URI channelUrl) {
        return channelUrl.getHost().contains("mirrativ.com");
    }
}
