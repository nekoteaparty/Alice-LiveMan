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

package site.alice.liveman.mediaproxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import site.alice.liveman.mediaproxy.proxytask.MediaProxyTask;
import site.alice.liveman.mediaproxy.proxytask.NicoLiveMediaProxyTask;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;


@Component
public class NicoLiveMediaProxy extends M3u8MediaProxy {

    @Override
    public boolean isMatch(URI url, String requestFormat) {
        return url.getHost().contains("nicovideo.jp") && url.getScheme().contains("wss") && requestFormat.equals("m3u8");
    }

    @Override
    public MediaProxyTask createProxyTask(String videoId, URI sourceUrl) {
        return new NicoLiveMediaProxyTask(videoId, sourceUrl);
    }
}
