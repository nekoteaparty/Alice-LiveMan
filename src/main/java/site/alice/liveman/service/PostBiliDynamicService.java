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
package site.alice.liveman.service;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import site.alice.liveman.event.MediaProxyEvent;
import site.alice.liveman.event.MediaProxyEventListener;
import site.alice.liveman.model.AccountInfo;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.mediaproxy.MediaProxyManager;
import site.alice.liveman.mediaproxy.proxytask.MediaProxyTask;
import site.alice.liveman.utils.HttpRequestUtil;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PostBiliDynamicService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostBiliDynamicService.class);

    @Autowired
    private LiveManSetting liveManSetting;

    private static final File   dynamicPostedListFile = new File("dynamicPostedList.txt");
    private static final String DYNAMIC_POST_API      = "https://api.vc.bilibili.com/dynamic_repost/v1/dynamic_repost/repost";
    private static final String DYNAMIC_POST_PARAM    = "dynamic_id=0&type=4&rid=0&content=#Vtuber##%s# 正在直播：%s https://live.bilibili.com/36577&at_uids=&ctrl=[]&csrf_token=";

    @PostConstruct
    public void init() {
        MediaProxyManager.addListener(new MediaProxyEventListener() {
            @Override
            public void onProxyStart(MediaProxyEvent e) {
                List<String> dynamicPostedList;
                try {
                    dynamicPostedList = FileUtils.readLines(dynamicPostedListFile, StandardCharsets.UTF_8);
                } catch (IOException ignore) {
                    dynamicPostedList = new ArrayList<>();
                }

            }
        });
    }
}
