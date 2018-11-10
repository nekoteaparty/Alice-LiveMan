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
package site.alice.liveman.web.rpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import site.alice.liveman.mediaproxy.MediaProxyManager;

@Controller
@RequestMapping("/mediaProxy")
public class MediaProxyController {

    private static final Logger logger = LoggerFactory.getLogger(MediaProxyController.class);

    @RequestMapping("/{proxyName}/{videoId}")
    public void mediaProxyHandler(@PathVariable String proxyName, @PathVariable String videoId) {
        try {
            MediaProxyManager.getMediaProxy(proxyName).requestHandler(videoId);
        } catch (Throwable e) {
            logger.error(e.getMessage());
        }
    }

}
