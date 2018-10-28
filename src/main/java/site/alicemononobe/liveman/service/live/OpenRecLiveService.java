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
public class OpenRecLiveService extends LiveService {
    @Override
    public VideoInfo getLiveVideoInfo(ChannelInfo channelInfo) throws Exception {
        String channelName = channelInfo.getChannelUrl().replace("https://www.openrec.tv/user/", "");
        URL moviesUrl = new URL("https://public.openrec.tv/external/api/v5/movies?channel_id=" + channelName + "&sort=onair_status");
        String moviesJson = HttpRequestUtil.downloadUrl(moviesUrl, StandardCharsets.UTF_8, null);
        JSONArray movies = JSON.parseArray(moviesJson);
        if (!movies.isEmpty()) {
            JSONObject movieObj = (JSONObject) movies.get(0);
            Integer onAirStatus = movieObj.getInteger("onair_status");
            if (onAirStatus == 1) {
                String videoId = movieObj.getString("id");
                String videoTitle = movieObj.getString("title");
                URL m3u8ListUrl = new URL(movieObj.getJSONObject("media").getString("url"));
                String[] m3u8List = HttpRequestUtil.downloadUrl(m3u8ListUrl, StandardCharsets.UTF_8, null).split("\n");
                String mediaUrl = null;
                for (int i = 0; i < m3u8List.length; i++) {
                    if (m3u8List[i].contains("1280x720")) {
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
        return channelUrl.getHost().contains("openrec.tv");
    }
}
