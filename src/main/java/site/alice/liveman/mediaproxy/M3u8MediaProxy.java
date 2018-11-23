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
import site.alice.liveman.mediaproxy.proxytask.M3u8MediaProxyTask;
import site.alice.liveman.mediaproxy.proxytask.MediaProxyTask;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.Proxy;
import java.net.URI;

@Component
public class M3u8MediaProxy implements MediaProxy {
    @Autowired
    private HttpServletResponse response;

    @Override
    public boolean isMatch(URI url, String requestFormat) {
        return url.getPath().endsWith(".m3u8") && requestFormat.equals("m3u8");
    }

    @Override
    public MediaProxyTask createProxyTask(String videoId, URI sourceUrl) {
        return new M3u8MediaProxyTask(videoId, sourceUrl);
    }

    @Override
    public void requestHandler(String videoId) {
        try {
            response.sendRedirect("/mediaProxy/temp/m3u8/" + videoId + "/index.m3u8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

