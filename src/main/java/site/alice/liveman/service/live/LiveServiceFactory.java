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
package site.alice.liveman.service.live;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

@Component
public class LiveServiceFactory implements ApplicationContextAware {
    private Map<String, LiveService> liveServiceMap;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        liveServiceMap = applicationContext.getBeansOfType(LiveService.class);
    }

    public LiveService getLiveService(String channelUrl) {
        for (LiveService liveService : liveServiceMap.values()) {
            try {
                if (liveService.isMatch(new URI(channelUrl))) {
                    return liveService;
                }
            } catch (Exception ignored) {
            }
        }
        throw new BeanDefinitionStoreException("没有找到可以处理[" + channelUrl + "]的LiveService");
    }
}
