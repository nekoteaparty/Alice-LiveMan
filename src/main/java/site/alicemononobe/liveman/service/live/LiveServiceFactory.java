package site.alicemononobe.liveman.service.live;

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

    public LiveService getLiveService(String channelUrl) throws URISyntaxException {
        for (LiveService liveService : liveServiceMap.values()) {
            if (liveService.isMatch(new URI(channelUrl))) {
                return liveService;
            }
        }
        throw new BeanDefinitionStoreException("没有找到可以处理[" + channelUrl + "]的LiveService");
    }
}
