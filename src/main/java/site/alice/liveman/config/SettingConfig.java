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

package site.alice.liveman.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.commons.io.IOUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import site.alice.liveman.model.AccountInfo;
import site.alice.liveman.model.LiveManSetting;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Configuration
public class SettingConfig {

    @Bean
    public LiveManSetting getLiveManSetting() throws IOException {
        LiveManSetting liveManSetting;
        File file = new File("setting.json");
        if (file.exists()) {
            try (InputStream is = new FileInputStream(file)) {
                String setting = IOUtils.toString(is, StandardCharsets.UTF_8);
                liveManSetting = JSON.parseObject(setting, LiveManSetting.class);
            }
        } else {
            liveManSetting = new LiveManSetting();
            liveManSetting.setAccounts(new CopyOnWriteArrayList<>());
            liveManSetting.setChannels(new CopyOnWriteArrayList<>());
            liveManSetting.setBannedKeywords(new String[0]);
            liveManSetting.setBannedYoutubeChannel(new String[0]);
            liveManSetting.setTempPath("liveManTemp");
            IOUtils.write(JSON.toJSONString(liveManSetting, SerializerFeature.PrettyFormat), new FileOutputStream(file));
        }
        return liveManSetting;
    }
}
