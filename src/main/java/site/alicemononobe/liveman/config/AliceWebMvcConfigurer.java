package site.alicemononobe.liveman.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import site.alicemononobe.liveman.mediaproxy.MediaProxyManager;

@Configuration
public class AliceWebMvcConfigurer implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/mediaProxy/temp/**").addResourceLocations("file:" + MediaProxyManager.getTempPath() + "/");
    }
}

