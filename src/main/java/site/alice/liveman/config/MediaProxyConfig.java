package site.alice.liveman.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import site.alice.liveman.mediaproxy.MediaProxyManager;

@Configuration
public class MediaProxyConfig {

    @Bean
    public MediaProxyManager getMediaProxyManager() {
        return new MediaProxyManager();
    }
}
