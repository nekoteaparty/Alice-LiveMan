package site.alice.liveman.service.live;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import site.alice.liveman.model.ChannelInfo;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.utils.HttpRequestUtil;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TwitcastingLiveService extends LiveService {

    private static final Logger  logger             = LoggerFactory.getLogger(TwitcastingLiveService.class);
    private static final Pattern ROOM_TITLE_PATTERN = Pattern.compile("<meta name=\"twitter:title\" content=\"(.+?)\">");

    @Override
    public VideoInfo getLiveVideoInfo(ChannelInfo channelInfo) throws Exception {
        String channelUrl = channelInfo.getChannelUrl();
        String roomName = channelUrl.replace("https://twitcasting.tv/", "").replace("/", "");
        URL streamServerUrl = new URL("https://twitcasting.tv/streamserver.php?target=" + roomName + "&mode=client");
        String serverInfo = HttpRequestUtil.downloadUrl(streamServerUrl, StandardCharsets.UTF_8, null);
        JSONObject streamServer = JSONObject.parseObject(serverInfo);
        JSONObject movie = streamServer.getJSONObject("movie");
        if (movie.getBoolean("live")) {
            String videoTitle = "";
            String roomHtml = HttpRequestUtil.downloadUrl(new URL(channelUrl), StandardCharsets.UTF_8, null);
            Matcher matcher = ROOM_TITLE_PATTERN.matcher(roomHtml);
            if (matcher.find()) {
                videoTitle = matcher.group(1);
            }
            String videoId = movie.getString("id");
            String mediaUrl = "wss://" + streamServer.getJSONObject("fmp4").getString("host") + "/ws.app/stream/" + videoId + "/fmp4/bd/1/1500?mode=main";
            return new VideoInfo(channelInfo, videoId, videoTitle, new URI(mediaUrl), "mp4");
        }
        return null;
    }

    @Override
    public boolean isMatch(URI channelUrl) {
        return channelUrl.getHost().contains("twitcasting.tv");
    }
}
